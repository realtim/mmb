package ru.mmb.sportiduinomanager;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;
import ru.mmb.sportiduinomanager.model.Teams;

import static ru.mmb.sportiduinomanager.StationMonitorService.DATA_UPDATED;
import static ru.mmb.sportiduinomanager.StationMonitorService.NO_DATA_IN_MSG;

/**
 * Provides ability to get Sportiduino records from station, mark team members
 * as absent, update team members mask in a chip and save this data
 * in local database.
 */
public final class ControlPointActivity extends MainActivity
        implements TeamListAdapter.OnTeamClicked, MemberListAdapter.OnMemberClicked {
    /**
     * User modified team members mask (it can be saved to chip and to local db).
     */
    private int mTeamMask;
    /**
     * Original team mask received from chip at last punch
     * or changed and saved previously.
     */
    private int mOriginalMask;

    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mMemberAdapter;
    /**
     * RecyclerView with list of teams punched at the station.
     */
    private TeamListAdapter mTeamAdapter;
    /**
     * Receiver of messages from station monitoring service.
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onStationDataUpdated(intent);
        }
    };

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        Log.d(StationAPI.CALLER_CP, "Create");
        setContentView(R.layout.activity_controlpoint);
        // Register receiver of messages from station monitoring service
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(DATA_UPDATED));
        // Start foreground service for monitoring of connected station for new punches
        startMonitoringService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(StationAPI.CALLER_CP, "Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(StationAPI.CALLER_CP, "Resume");
        // Wake the device (if the activity was started by monitoring service)
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock =
                powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        getString(R.string.app_name));
        wakeLock.acquire(5000);
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.control_point).setChecked(true);
        updateMenuItems(R.id.control_point);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Set flag for monitoring service in main app
        MainApp.setCPActivityActive(true);
        // Initialize masks
        mTeamMask = MainApp.getTeamMask();
        mOriginalMask = 0;
        // Prepare recycler view of members list
        final RecyclerView membersList = findViewById(R.id.cp_member_list);
        final RecyclerView.LayoutManager membersLM = new LinearLayoutManager(this);
        membersList.setLayoutManager(membersLM);
        // specify an RecyclerView adapter and initialize it
        mMemberAdapter = new MemberListAdapter(this,
                ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()),
                ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        membersList.setAdapter(mMemberAdapter);
        // Prepare recycler view of team list
        final RecyclerView teamsList = findViewById(R.id.cp_team_list);
        final RecyclerView.LayoutManager teamsLM = new LinearLayoutManager(this);
        teamsList.setLayoutManager(teamsLM);
        // Specify an RecyclerView adapter and initialize it
        mTeamAdapter = new TeamListAdapter(this, MainApp.mTeams, MainApp.mPointPunches);
        teamsList.setAdapter(mTeamAdapter);
        // Restore team list position and update activity layout
        int restoredPosition = MainApp.getTeamListPosition();
        int maxPosition = MainApp.mPointPunches.size() - 1;
        if (maxPosition < 0) maxPosition = 0;
        if (restoredPosition > maxPosition) {
            restoredPosition = maxPosition;
            MainApp.setTeamListPosition(restoredPosition);
        }
        updateMasks(true, restoredPosition);
        mTeamAdapter.setPosition(restoredPosition);
        updateLayout();
    }

    @Override
    protected void onPause() {
        Log.d(StationAPI.CALLER_CP, "Pause");
        // Set flag for monitoring service in main app
        MainApp.setCPActivityActive(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(StationAPI.CALLER_CP, "Stop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(StationAPI.CALLER_CP, "Destroy");
        // Stop monitoring service
        stopMonitoringService();
        // Unregister the receiver of messages from monitoring service
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    /**
     * The onClick implementation of TeamListAdapter RecyclerView item click.
     */
    @Override
    public void onTeamClick(final int position) {
        // Set masks for selected team
        updateMasks(false, position);
        // Change position in team list
        final int oldPosition = mTeamAdapter.getPosition();
        mTeamAdapter.setPosition(position);
        // Save new position and mask in main application
        MainApp.setTeamListPosition(position);
        MainApp.setTeamMask(mTeamMask);
        // Update team list
        mTeamAdapter.notifyItemChanged(oldPosition);
        mTeamAdapter.notifyItemChanged(position);
        // Update layout to display new selected team
        updateLayout();
    }

    /**
     * The onClick implementation of the RecyclerView team member click.
     */
    @Override
    public void onMemberClick(final int position) {
        // Compute new team mask
        final int newMask = mTeamMask ^ (1 << position);
        if (newMask == 0) {
            Toast.makeText(getApplicationContext(), R.string.err_init_empty_team, Toast.LENGTH_LONG).show();
            mMemberAdapter.notifyItemChanged(position);
            return;
        }
        mTeamMask = newMask;
        // Save it in main application
        MainApp.setTeamMask(mTeamMask);
        // Update list item
        mMemberAdapter.setMask(mTeamMask);
        mMemberAdapter.notifyItemChanged(position);
        // Display new team members count
        final int teamMembersCount = Teams.getMembersCount(mTeamMask);
        ((TextView) findViewById(R.id.cp_members_count)).setText(getResources()
                .getString(R.string.team_members_count, teamMembersCount));
        // Enable 'Save mask' button if new mask differs from original
        updateMaskButton();
    }

    /**
     * Background thread for periodic querying of connected station.
     */
    private void startMonitoringService() {
        MainApp.mStation.setQueryingAllowed(true);
        // Return if the service is already running
        final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (StationMonitorService.class.getName().equals(service.service.getClassName())) return;
        }
        // Start the service
        Log.d(StationAPI.CALLER_CP, "Start service");
        final Intent intent = new Intent(this, StationMonitorService.class);
        startService(intent);
    }

    /**
     * Stops rescheduling of periodic station query.
     */
    private void stopMonitoringService() {
        MainApp.mStation.setQueryingAllowed(false);
        Log.d(StationAPI.CALLER_CP, "Stop service");
        final Intent intent = new Intent(this, StationMonitorService.class);
        stopService(intent);
    }

    /**
     * Save changed team mask to chip amd to local database.
     *
     * @param view View of button clicked (unused)
     */
    public void saveTeamMask(@SuppressWarnings("unused") final View view) {
        // Check team mask and station presence
        if (mTeamMask == 0 || MainApp.mStation == null || MainApp.mPointPunches.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Get team number
        final int index = MainApp.mPointPunches.size() - 1 - mTeamAdapter.getPosition();
        final int teamNumber = MainApp.mPointPunches.getTeamNumber(index);
        // Add new record to global list with same point time and new mask
        if (!MainApp.mAllRecords.updateTeamMask(teamNumber, mTeamMask, MainApp.mStation, MainApp.mDatabase, false)) {
            Toast.makeText(getApplicationContext(), R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Replace punch with same record with new mask
        if (!MainApp.mPointPunches.updateTeamMask(teamNumber, mTeamMask, MainApp.mStation, MainApp.mDatabase, true)) {
            Toast.makeText(getApplicationContext(), R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Send command to station
        stopMonitoringService();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        MainApp.mStation.waitForQuerying2Stop();
        if (!MainApp.mStation.updateTeamMask(teamNumber, MainApp.mPointPunches.getInitTime(index), mTeamMask,
                StationAPI.CALLER_CP)) {
            Toast.makeText(getApplicationContext(), MainApp.mStation.getLastError(true), Toast.LENGTH_LONG).show();
            startMonitoringService();
            return;
        }
        startMonitoringService();
        // Rebuild masks class members
        updateMasks(false, mTeamAdapter.getPosition());
        // Disable button after successful saving of new mask
        updateMaskButton();
        // Update list of team members and their selection
        final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
        mMemberAdapter.updateList(teamMembers, mOriginalMask, mTeamMask);
    }

    /**
     * Update mOriginalMask and mTeamMask for selected team.
     *
     * @param restore  True if we are starting to work with new team,
     *                 False if we are restoring mask after activity restart
     * @param position Position of selected team in the list
     */
    private void updateMasks(final boolean restore, final int position) {
        // Get the original mask of selected team
        final int index = MainApp.mPointPunches.size() - 1 - position;
        mOriginalMask = MainApp.mPointPunches.getTeamMask(index);
        if (restore) {
            // Restore current mask from main application
            mTeamMask = MainApp.getTeamMask();
            // Set it to original if the mask in main application was not initialized
            if (mTeamMask == 0) {
                mTeamMask = mOriginalMask;
                MainApp.setTeamMask(mTeamMask);
            }
        } else {
            // Set current mask equal to original from database
            mTeamMask = mOriginalMask;
            MainApp.setTeamMask(mTeamMask);
        }
    }

    /**
     * Update layout with new data received from connected station.
     *
     * @param intent Intent with data sent from monitoring service
     */
    private void onStationDataUpdated(final Intent intent) {
        // Extract data from the message received from monitoring service
        final int result = intent.getIntExtra("result", NO_DATA_IN_MSG);
        final int selectedTeamN = intent.getIntExtra("selectedTeamN", NO_DATA_IN_MSG);
        if (result == NO_DATA_IN_MSG || selectedTeamN == NO_DATA_IN_MSG) {
            Toast.makeText(getApplicationContext(), R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Update station clock in UI
        ((TextView) findViewById(R.id.station_clock)).setText(
                Records.printTime(MainApp.mStation.getStationTime(), "dd.MM  HH:mm:ss"));
        // Do nothing if no new data has been arrived
        if (result == 0) return;
        // Update layout if new data has been arrived and/or error has been occurred
        if (mTeamAdapter.getPosition() == 0) {
            // Reset current mask if we at first item of team list
            // as it is replaced with new team just arrived
            updateMasks(false, 0);
        } else {
            // Change position in the list to keep current team selected
            int newPosition = 0;
            final int listSize = MainApp.mPointPunches.size();
            for (int i = 0; i < listSize; i++) {
                if (MainApp.mPointPunches.getTeamNumber(i) == selectedTeamN) {
                    newPosition = listSize - i - 1;
                    break;
                }
            }
            mTeamAdapter.setPosition(newPosition);
            MainApp.setTeamListPosition(newPosition);
        }
        // Update team list as we have a new team in it
        mTeamAdapter.notifyDataSetChanged();
        // Update activity layout as some elements has been changed
        updateLayout();
        // Menu should be changed if we have new records unsent to site
        updateMenuItems(R.id.control_point);
        // Display station communication error (if any)
        if (result > 0) Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
    }

    /**
     * Update layout according to activity state.
     */
    private void updateLayout() {
        // Update number of teams punched at this control point
        // (station clock will be updated in background thread)
        ((TextView) findViewById(R.id.cp_total_teams)).setText(getResources()
                .getString(R.string.cp_total_teams, MainApp.mPointPunches.size()));
        // Hide last team block when no teams have been punched at the station
        if (MainApp.mPointPunches.isEmpty()) {
            findViewById(R.id.cp_team_data).setVisibility(View.GONE);
            return;
        }
        // Get index of our team in mPointPunches team punches list
        final int index = MainApp.mPointPunches.size() - 1 - mTeamAdapter.getPosition();
        // Update team number and name
        final int teamNumber = MainApp.mPointPunches.getTeamNumber(index);
        final String teamName = MainApp.mTeams.getTeamName(teamNumber);
        ((TextView) findViewById(R.id.cp_team_name)).setText(getResources()
                .getString(R.string.cp_team_name, teamNumber, teamName));
        // Update team time
        ((TextView) findViewById(R.id.cp_team_time)).setText(
                Records.printTime(MainApp.mPointPunches.getTeamTime(index), "dd.MM  HH:mm:ss"));
        // Update lists of punched points
        final List<Integer> punched = MainApp.mAllRecords.getChipPunches(teamNumber,
                MainApp.mPointPunches.getInitTime(index), MainApp.mStation.getNumber(),
                MainApp.mStation.getMACasLong(), 255);
        ((TextView) findViewById(R.id.cp_punched)).setText(getResources()
                .getString(R.string.cp_punched, MainApp.mDistance.pointsNamesFromList(punched)));
        // Update lists of skipped points
        final List<Integer> skipped = MainApp.mDistance.getSkippedPoints(punched);
        final TextView skippedText = findViewById(R.id.cp_skipped);
        skippedText.setText(getResources().getString(R.string.cp_skipped,
                MainApp.mDistance.pointsNamesFromList(skipped)));
        if (MainApp.mDistance.mandatoryPointSkipped(skipped)) {
            skippedText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        } else {
            skippedText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()));
        }
        // Update saveTeamMask button state
        updateMaskButton();
        // Get list of team members
        final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
        // Update list of team members and their selection
        mMemberAdapter.updateList(teamMembers, mOriginalMask, mTeamMask);
        // Update number of team members
        ((TextView) findViewById(R.id.cp_members_count)).setText(getResources()
                .getString(R.string.team_members_count, Teams.getMembersCount(mTeamMask)));
        // Show last team block
        findViewById(R.id.cp_team_data).setVisibility(View.VISIBLE);
    }

    /**
     * Enable "Register dismiss" button if team mask was changed by user.
     */
    private void updateMaskButton() {
        final Button saveMaskButton = findViewById(R.id.cp_save_mask);
        if (mTeamMask == mOriginalMask) {
            saveMaskButton.setAlpha(MainApp.DISABLED_BUTTON);
            saveMaskButton.setClickable(false);
        } else {
            saveMaskButton.setAlpha(MainApp.ENABLED_BUTTON);
            saveMaskButton.setClickable(true);
        }
    }
}
