package ru.mmb.sportiduinomanager.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import ru.mmb.sportiduinomanager.BluetoothActivity;
import ru.mmb.sportiduinomanager.MainApplication;
import ru.mmb.sportiduinomanager.model.Chips;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Run long station reset in separate thread.
 */
public class ResetStationTask extends AsyncTask<Byte, Long, Boolean> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<BluetoothActivity> mActivityRef;
    /**
     * Reference to main application thread.
     */
    private final MainApplication mMainApplication;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ResetStationTask(final BluetoothActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApplication) context.getApplication();
    }

    /**
     * Change station number via station reset.
     *
     * @param newNumbers New station number
     * @return True if succeeded
     */
    protected Boolean doInBackground(final Byte... newNumbers) {
        // TODO: return int with id of specific error message, not boolean
        final Station station = mMainApplication.getStation();
        if (station == null) return false;
        // Update station status to get number of visited team and last time visit
        if (!station.fetchStatus()) return false;
        // Check if some teams has been visited the station while it had it's old number
        final int chipsRegistered = station.getChipsRegistered();
        final Teams teams = mMainApplication.getTeams();
        final Chips chips = mMainApplication.getChips();
        int teamsRescanned = 0;
        // Compute total time without teams scan
        long totalTime = estimateTimeToComplete(0, 0, station.getNumber());
        if (station.getNumber() != 0 && chipsRegistered > 0 && teams != null && chips != null) {
            // Get all teams number from local database
            final List<Integer> teamList = teams.getTeamList();
            // Update total time estimate
            totalTime = estimateTimeToComplete(teamList.size(), chipsRegistered, station.getNumber());
            publishProgress(totalTime, totalTime);
            // Rescan all teams from the list
            for (int i = 0; i < teamList.size(); i++) {
                final int teamNumber = teamList.get(i);
                publishProgress(estimateTimeToComplete(teamList.size() - i,
                        chipsRegistered - teamsRescanned, station.getNumber()), totalTime);
                // Fetch data for the team visit to station
                if (!station.fetchTeamRecord(teamNumber)) continue;
                final Chips teamVisit = station.getChipEvents();
                if (teamVisit.size() == 0) continue;
                if (teamNumber != teamVisit.getTeamNumber(0)) continue;
                final long initTime = teamVisit.getInitTime(0);
                final int teamMask = teamVisit.getTeamMask(0);
                // Read marks from chip and to events list
                final int marks = station.getChipRecordsN();
                if (marks == 0) continue;
                teamsRescanned++;
                int fromMark = 0;
                do {
                    int toRead = marks;
                    if (toRead > Station.MAX_MARK_COUNT) {
                        toRead = Station.MAX_MARK_COUNT;
                    }
                    if (!station.fetchTeamMarks(teamNumber, initTime, teamMask, fromMark, toRead)) {
                        break;
                    }
                    fromMark += toRead;
                    // Add fetched chip marks to local list of events
                    chips.join(station.getChipEvents());
                } while (fromMark < marks);
                // Stop scanned if we found all visited teams
                if (teamsRescanned == chipsRegistered) break;
            }
            // Save fetched events in local database
            final String result = chips.saveNewEvents(mMainApplication.getDatabase());
            if (!"".equals(result)) return false;
            // Copy changed list of chips events to main application
            mMainApplication.setChips(chips, false);
        }
        publishProgress(estimateTimeToComplete(0, 0, station.getNumber()), totalTime);
        // Reset station to change it's number
        final byte newNumber = newNumbers[0];
        if (!station.resetStation(newNumber)) return false;
        // Sleep for 0.5 second while station is rebooting after reset
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Update station mode if needed
        publishProgress(0L, totalTime);
        final byte newMode = newNumbers[1];
        if (newMode != station.getMode()) {
            // Send newMode command
            return station.newMode(newMode);
        }
        return true;
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
     * @param result False if station reset failed
     */
    protected void onPostExecute(final Boolean result) {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Process reset result in UI activity
        activity.onStationResetResult(result);
    }

    /**
     * Estimates time in milliseconds left during station reset process.
     *
     * @param teamsToScan     Number of teams to check with fetchTeamRecord
     * @param teamsWithVisits Number of teams to check with fetchTeamMarks
     * @param pointNumber     Point number (to estimate N of calls to fetchTeamMarks)
     * @return Estimated time in ms
     */
    private long estimateTimeToComplete(final int teamsToScan, final int teamsWithVisits,
                                        final int pointNumber) {
        final int marksScans = pointNumber / (Station.MAX_MARK_COUNT - 1) + 1;
        return teamsToScan * 150 + teamsWithVisits * marksScans * 150 + 24_000 + 500;
    }

}
