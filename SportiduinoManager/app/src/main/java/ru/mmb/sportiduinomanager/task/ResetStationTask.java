package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import ru.mmb.sportiduinomanager.BluetoothActivity;
import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.R;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Run long station reset in separate thread.
 */
public class ResetStationTask extends AsyncTask<Integer, Long, Integer> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<BluetoothActivity> mActivityRef;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ResetStationTask(final BluetoothActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
    }

    /**
     * Change station number via station reset.
     *
     * @param newNumbers New station number
     * @return R.string error code or zero if succeeded
     */
    protected Integer doInBackground(final Integer... newNumbers) {
        if (MainApp.mStation == null) return R.string.err_station_absent;
        // Update station status to get number of punched teams and time of last punch
        if (!MainApp.mStation.fetchStatus()) return MainApp.mStation.getLastError(true);
        // Check if some teams punched at the station while it had it's old number
        final int chipsRegistered = MainApp.mStation.getChipsRegistered();
        // Compute total time without teams scan
        long totalTime = estimateTimeToComplete(0, 0, MainApp.mStation.getNumber());
        if (MainApp.mStation.getNumber() != 0 && chipsRegistered > 0) {
            // Get all teams number from local database
            final List<Integer> teamList = MainApp.mTeams.getTeamList();
            // Update total time estimate
            totalTime = estimateTimeToComplete(teamList.size(), chipsRegistered, MainApp.mStation.getNumber());
            publishProgress(totalTime, totalTime);
            // Scan all teams from local database
            final int error = rescanTeams(teamList, chipsRegistered, totalTime);
            if (error != 0) return error;
        }
        publishProgress(estimateTimeToComplete(0, 0, MainApp.mStation.getNumber()), totalTime);
        // Reset station to change it's number
        final int newNumber = newNumbers[0];
        if (!MainApp.mStation.resetStation(newNumber)) return MainApp.mStation.getLastError(true);
        // Sleep for 0.5 second while station is rebooting after reset
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Update station mode if needed
        publishProgress(0L, totalTime);
        final int newMode = newNumbers[1];
        if (newMode != MainApp.mStation.getMode() && !MainApp.mStation.newMode(newMode)) {
            return MainApp.mStation.getLastError(true);
        }
        return 0;
    }

    /**
     * Rescan station for all registered to the raid teams.
     *
     * @param teamList        List of all teams in local database
     * @param chipsRegistered Number of teams which has been punched at the station
     * @param totalTime       Estimated total time of station reset
     * @return R.string error code or zero if succeeded
     */
    private int rescanTeams(final List<Integer> teamList, final int chipsRegistered, final long totalTime) {
        // Rescan all teams from the list
        int teamsRescanned = 0;
        for (int i = 0; i < teamList.size(); i++) {
            final int teamNumber = teamList.get(i);
            publishProgress(estimateTimeToComplete(teamList.size() - i,
                    chipsRegistered - teamsRescanned, MainApp.mStation.getNumber()), totalTime);
            // Fetch data for the team punch at the station
            if (!MainApp.mStation.fetchTeamRecord(teamNumber)) {
                final int error = MainApp.mStation.getLastError(true);
                if (error == R.string.err_station_no_data
                        || error == R.string.err_station_flash_empty) continue;
                return error;
            }
            final Records teamPunches = MainApp.mStation.getTeamPunches();
            if (teamPunches.size() == 0) continue;
            if (teamNumber != teamPunches.getTeamNumber(0)) continue;
            final long initTime = teamPunches.getInitTime(0);
            final int teamMask = teamPunches.getTeamMask(0);
            // Read punches from chip and to the all records list in application memory
            final int marks = MainApp.mStation.getChipRecordsN();
            if (marks == 0) continue;
            teamsRescanned++;
            int fromMark = 0;
            do {
                int toRead = marks;
                if (toRead > Station.MAX_MARK_COUNT) {
                    toRead = Station.MAX_MARK_COUNT;
                }
                if (!MainApp.mStation.fetchTeamMarks(teamNumber, initTime, teamMask, fromMark, toRead)) {
                    final int error = MainApp.mStation.getLastError(true);
                    if (error != R.string.err_station_flash_empty) return error;
                }
                fromMark += toRead;
                // Add fetched punches from the chip to local list of records
                MainApp.mAllRecords.join(MainApp.mStation.getTeamPunches());
            } while (fromMark < marks);
            // Stop scanned if we found all punched teams
            if (teamsRescanned == chipsRegistered) break;
        }
        // Save fetched records in local database
        final String result = MainApp.mAllRecords.saveNewRecords(MainApp.mDatabase);
        if (!"".equals(result)) return R.string.err_db_sql_error;
        return 0;
    }

    /**
     * Update activity layout to show task progress.
     * Called from publishProgress in UI context.
     *
     * @param progress Current and total estimated time to completion, ms
     */
    protected void onProgressUpdate(final Long... progress) {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Update progress in percents and time to completion
        final int percents = (int) (100L - 100L * progress[0] / progress[1]);
        final int secondsToComplete = (int) (progress[0] / 1000L);
        activity.updateResetProgress(percents, secondsToComplete);
    }

    /**
     * Show error message in case of station reset failure and update screen layout.
     *
     * @param result Zero or error code if station reset has been failed
     */
    protected void onPostExecute(final Integer result) {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Process reset result in UI activity
        activity.onStationResetResult(result);
    }

    /**
     * Estimates time in milliseconds left during station reset process.
     *
     * @param teamsToScan      Number of teams to check with fetchTeamRecord
     * @param teamsWithPunches Number of teams to check with fetchTeamMarks
     * @param pointNumber      Point number (to estimate N of calls to fetchTeamMarks)
     * @return Estimated time in ms
     */
    private long estimateTimeToComplete(final int teamsToScan, final int teamsWithPunches,
                                        final int pointNumber) {
        final int marksScans = pointNumber / (Station.MAX_MARK_COUNT - 1) + 1;
        return teamsToScan * 150 + teamsWithPunches * marksScans * 150 + 24_000 + 500;
    }

}
