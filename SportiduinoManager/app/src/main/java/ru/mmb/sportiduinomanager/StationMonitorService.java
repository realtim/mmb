package ru.mmb.sportiduinomanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;

/**
 * Provides foreground service for querying connected station every second
 * for new chips punches.
 */
public class StationMonitorService extends Service {

    /**
     * ID of "data updated" notification messages for ControlPointActivity.
     */
    static final String DATA_UPDATED = "data-from-station-updated";

    /**
     * ID of "progress bar updated" notification messages for ControlPointActivity.
     */
    static final String PROGRESS_UPDATED = "progress-bar-updated";

    /**
     * Default value for parsing messages from service
     * indicating that no data is present in the message.
     */
    static final int NO_DATA_IN_MSG = -2;

    /**
     * Service context for sending messages to activity.
     */
    private Context mContext;

    /**
     * Timer for running a task every second.
     */
    private Timer mTimer;

    /**
     * Create foreground service.
     **/
    @Override
    public void onCreate() {
        super.onCreate();
        // save service context for sending messages to activity
        mContext = this;
        // prepare notification that will be shown to user about service start
        final Intent notifyIntent = new Intent(this, ControlPointActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notifyIntent, PendingIntent.FLAG_IMMUTABLE);
        final String channelId = BuildConfig.APPLICATION_ID;
        final Notification startNotification =
                new NotificationCompat.Builder(this, channelId)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.monitoring_service))
                        .setColor(getResources().getColor(R.color.bg_primary))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                        .setContentIntent(pendingIntent)
                        .build();
        // in Android >= 8.0 (API 26) all notifications must be assigned to a channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(channelId, getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.monitoring_service));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            final NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        // start service in foreground
        startForeground(1, startNotification);
    }

    /**
     * Called by the system every time a client explicitly starts the service
     * by calling Context.startService(Intent).
     *
     * @param intent  The Intent supplied to Context.startService(Intent)
     * @param flags   Additional data about this start request
     * @param startId A unique integer representing this specific request to start
     * @return Describes what the system should do if the service was killed
     */
    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        // Stop the timer if it was started already
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        // Start new TimerTask
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            /**
             * Run every 1s, get station status and get new team data (if any).
             */
            @Override
            public void run() {
                // Station was not connected yet
                if (MainApp.mStation == null) return;
                // Querying is scheduled to stop, return immediately
                if (!MainApp.mStation.isQueryingAllowed()) return;
                // Inform other activities that service starts sending queries to station
                MainApp.mStation.setQueryingActive(true);
                // Save currently selected team
                final int selectedTeamN = MainApp.mPointPunches
                        .getTeamNumber(MainApp.mPointPunches.size() - 1 - MainApp.UI_STATE.getTeamListPosition());
                // Fetch current station status
                MainApp.mStation.fetchStatus();
                // Get the latest data from connected station
                final int result = fetchTeamsPunches();
                // Inform other activities that service finished sending queries to station
                MainApp.mStation.setQueryingActive(false);
                // Send notification to ControlPointActivity - time to update UI
                updateTeamList(result, selectedTeamN);
                // Force ControlPointActivity into foreground in case of new data or error
                if (result != 0 && !MainApp.isCPActivityActive()) {
                    // Bring activity in foreground
                    final Intent activityIntent = new Intent(getApplicationContext(), ControlPointActivity.class);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    getApplicationContext().startActivity(activityIntent);
                    // Mark it already started in advance to prevent multiple start requests
                    MainApp.setCPActivityActive(true);
                }
            }
        }, 0, 1000);
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    /**
     * Detect what teams can have new data since last check.
     * Method is based upon time of last punch, number of punched teams and
     * the difference between station list of last punched teams and local teams data
     *
     * @return List of teams which data should be fetched from station
     */
    private List<Integer> getTeamsToFetch() {
        List<Integer> fetchTeams = new ArrayList<>();

        // Do nothing if no teams has been punched yet
        if (MainApp.mStation.getLastPunchTime() == 0) return fetchTeams;
        // Do nothing if time of last punch in local db and at station is the same
        // (it can change without changing of number of teams)
        final int localTeamsCount = MainApp.mPointPunches.size();
        final long localLastPunch = MainApp.mPointPunches.getTeamTime(localTeamsCount - 1);
        if (localLastPunch == MainApp.mStation.getLastPunchTime()) return fetchTeams;

        // Ok, we have some new punches, we shouldn't make full station scan in most cases
        boolean fullDownload = !MainApp.mStation.fetchLastTeams();
        // Ask station for new teams list
        final List<Integer> stationLastTeams = MainApp.mStation.getLastTeams();
        // If station list is full,  then we need to make a full scan
        if (stationLastTeams.size() >= StationAPI.LAST_TEAMS_LEN) fullDownload = true;
        // If station list is empty but last team time has been changed,
        // then we have a fatal error and need full scan
        if (stationLastTeams.isEmpty()) fullDownload = true;
        // For full rescan of all teams make a list of all registered teams
        // Otherwise we need only to check new teams since previous check
        if (fullDownload) {
            fetchTeams = MainApp.mTeams.getTeamList();
        } else {
            fetchTeams = stationLastTeams;
        }
        return fetchTeams;
    }

    /**
     * Get all punches for all new teams
     * which has been punched at the station after the last check.
     * Returns -1 if some new teams has been punched at the station,
     * 0 if no new teams has been punched,
     * > 0 indicates an error code.
     *
     * @return -1/0/error code
     */
    private int fetchTeamsPunches() {
        // Build the list of teams to fetch their data
        final List<Integer> fetchTeams = getTeamsToFetch();
        if (fetchTeams.isEmpty()) return 0;
        final List<Integer> currLastTeams = MainApp.mStation.getLastTeams();
        // Get Sportiduino records for all teams in fetch list
        boolean flashChanged = false;
        boolean newRecords = false;
        int stationError = 0;
        final int teamListSize = fetchTeams.size();
        final boolean longScan = teamListSize > 1;
        for (int n = 0; n < teamListSize; n++) {
            // Display progress bar in ControlPointActivity for long scans
            if (longScan) updateProgressBar(n, teamListSize);
            // Fetch data for the team punched at the station
            final int teamNumber = fetchTeams.get(n);
            int newError = 0;
            if (!MainApp.mStation.fetchTeamHeader(teamNumber)) {
                newError = MainApp.mStation.getLastError(true);
                // Ignore data absence for teams which are not in last teams list
                // Most probable these teams did not punched at the station at all
                if (newError == R.string.err_station_no_data && !currLastTeams.contains(teamNumber)) continue;
                // Abort scanning in case of serious error
                // Continue scanning in case of problems with copying data from chip to memory
                if (newError != R.string.err_station_flash_empty && newError != R.string.err_station_no_data) {
                    return newError;
                }
            }
            // Get team punches as a Sportiduino record list
            final Records teamPunches = MainApp.mStation.getRecords();
            if (teamPunches.isEmpty()) {
                // Team punch was not registered at all due to err_station_no_data error
                // Create synthetic team punch with zero chip init time
                final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
                int originalMask = 0;
                for (int i = 0; i < teamMembers.size(); i++) {
                    originalMask = originalMask | (1 << i);
                }
                teamPunches.addRecord(MainApp.mStation, MainApp.mStation.getMode(), 0, teamNumber, originalMask,
                        MainApp.mStation.getNumber(), MainApp.mStation.getLastPunchTime());
            }
            // Prepare to clone init time and mask from this record to punches from the chip
            if (teamNumber != teamPunches.getTeamNumber(0)) return R.string.err_station_team_changed;
            final long initTime = teamPunches.getInitTime(0);
            final int teamMask = teamPunches.getTeamMask(0);
            // Update persistent list of punches at current control point
            if (MainApp.mPointPunches.merge(teamPunches)) {
                flashChanged = true;
            }
            // Try to add team punches as new records
            if (MainApp.mAllRecords.join(teamPunches)) {
                newRecords = true;
                // Read punches from chip and to record list
                final int marks = MainApp.mStation.getChipRecordsN();
                int fromMark = 0;
                do {
                    int toRead = marks - fromMark;
                    if (toRead <= 0) break;
                    if (toRead > StationAPI.MAX_PUNCH_COUNT) {
                        toRead = StationAPI.MAX_PUNCH_COUNT;
                    }
                    if (!MainApp.mStation.fetchTeamPunches(teamNumber, initTime, teamMask, fromMark, toRead)) {
                        newError = MainApp.mStation.getLastError(true);
                        if (newError != R.string.err_station_flash_empty) return newError;
                    }
                    fromMark += toRead;
                    // Add fetched punches to application list of records
                    MainApp.mAllRecords.join(MainApp.mStation.getRecords());
                } while (fromMark < marks);
            }
            // Save non-fatal station error
            if (newError == R.string.err_station_flash_empty || newError == R.string.err_station_no_data) {
                stationError = newError;
            }
        }
        // Hide previously shown progress bar in ControlPointActivity for long scans
        if (longScan) updateProgressBar(0, 0);
        // We have asked station for all teams from fetch list
        // Save new records (if any) to local db
        if (newRecords) {
            // Save new records in local database
            final String result = MainApp.mAllRecords.saveNewRecords(MainApp.mDatabase);
            if (!"".equals(result)) return R.string.err_db_sql_error;
        }
        // Sort punches by their time
        if (flashChanged) MainApp.mPointPunches.sort();
        // Report non-fatal errors which has been occurred during scanning
        if (stationError != 0) return stationError;
        // Report 'data changed' for updating UI
        if (newRecords || flashChanged) {
            return -1;
        } else {
            return 0;
        }
    }

    private void updateProgressBar(final int currentTeam, final int totalTeams) {
        final Intent intent = new Intent(PROGRESS_UPDATED);
        intent.putExtra("current", currentTeam);
        intent.putExtra("total", totalTeams);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void updateTeamList(final int result, final int selectedTeamN) {
        final Intent intent = new Intent(DATA_UPDATED);
        intent.putExtra("result", result);
        intent.putExtra("selectedTeamN", selectedTeamN);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    /**
     * Create simple unbound service.
     */
    @Override
    public IBinder onBind(final Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    /**
     * Stop querying connected station on service destroy.
     */
    @Override
    public void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        stopSelf();
        super.onDestroy();
    }
}
