package ru.mmb.sportiduinomanager;

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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.mmb.sportiduinomanager.adapter.MemberListAdapter;
import ru.mmb.sportiduinomanager.adapter.TeamListAdapter;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;
import ru.mmb.sportiduinomanager.model.Teams;

import static ru.mmb.sportiduinomanager.StationMonitorService.DATA_UPDATED;
import static ru.mmb.sportiduinomanager.StationMonitorService.NO_DATA_IN_MSG;
import static ru.mmb.sportiduinomanager.StationMonitorService.PROGRESS_UPDATED;

/**
 * Provides ability to get Sportiduino records from station, mark team members
 * as absent, update team members mask in a chip and save this data
 * in local database.
 */
public final class ControlPointActivity extends MenuActivity
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
     * True when service is fetching a lot of teams from station
     * and progress bar should be visible.
     */
    private boolean mLongScan;
    /**
     * Receiver of "full scan" messages from station monitoring service.
     */
    private final BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onScanProgressChanged(intent);
        }
    };
    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mMemberAdapter;
    /**
     * RecyclerView with list of teams punched at the station.
     */
    private TeamListAdapter mTeamAdapter;
    /**
     * Receiver of "data changed" messages from station monitoring service.
     */
    private final BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onStationDataUpdated(intent);
        }
    };

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        setContentView(R.layout.activity_controlpoint);
        // Register receiver of messages from station monitoring service
        LocalBroadcastManager.getInstance(this).registerReceiver(mDataReceiver,
                new IntentFilter(DATA_UPDATED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressReceiver,
                new IntentFilter(PROGRESS_UPDATED));
        // Start foreground service for monitoring of connected station for new punches
        startMonitoringService();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        mTeamMask = MainApp.UI_STATE.getTeamMask();
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
        int restoredPosition = MainApp.UI_STATE.getTeamListPosition();
        int maxPosition = MainApp.mPointPunches.size() - 1;
        if (maxPosition < 0) maxPosition = 0;
        if (restoredPosition > maxPosition) {
            restoredPosition = maxPosition;
            MainApp.UI_STATE.setTeamListPosition(restoredPosition);
        }
        updateMasks(true, restoredPosition);
        mTeamAdapter.setPosition(restoredPosition);
        updateLayout();
    }

    @Override
    protected void onPause() {
        // Set flag for monitoring service in main app
        MainApp.setCPActivityActive(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Stop monitoring service
        stopMonitoringService();
        // Unregister the receiver of messages from monitoring service
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDataReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressReceiver);
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
        MainApp.UI_STATE.setTeamListPosition(position);
        MainApp.UI_STATE.setTeamMask(mTeamMask);
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
        MainApp.UI_STATE.setTeamMask(mTeamMask);
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
        if (!MainApp.mStation.updateTeamMask(teamNumber, MainApp.mPointPunches.getInitTime(index), mTeamMask)) {
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
            mTeamMask = MainApp.UI_STATE.getTeamMask();
            // Set it to original if the mask in main application was not initialized
            if (mTeamMask == 0) {
                mTeamMask = mOriginalMask;
                MainApp.UI_STATE.setTeamMask(mTeamMask);
            }
        } else {
            // Set current mask equal to original from database
            mTeamMask = mOriginalMask;
            MainApp.UI_STATE.setTeamMask(mTeamMask);
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
            MainApp.UI_STATE.setTeamListPosition(newPosition);
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
     * Update progress bar for full station scan.
     *
     * @param intent Intent with data sent from monitoring service
     */
    private void onScanProgressChanged(final Intent intent) {
        // Extract data from the message received from monitoring service
        final int currentTeam = intent.getIntExtra("current", NO_DATA_IN_MSG);
        final int totalTeams = intent.getIntExtra("total", NO_DATA_IN_MSG);
        if (currentTeam == NO_DATA_IN_MSG || totalTeams == NO_DATA_IN_MSG) {
            Toast.makeText(getApplicationContext(), R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (totalTeams == 0) {
            // Scanning has been ended, hide the progress bar
            findViewById(R.id.cp_full_scan).setVisibility(View.GONE);
            mLongScan = false;
            // Don't show team data, layout has not been updated yet
            return;
        }
        mLongScan = true;
        if (currentTeam == 0) {
            // Scanning is starting, show the progress bar
            findViewById(R.id.cp_team_data).setVisibility(View.GONE);
            findViewById(R.id.cp_full_scan).setVisibility(View.VISIBLE);
        }
        // Compute progress in percents left
        final int percents = (int) (100L * currentTeam / totalTeams);
        // Compute approximate number of fetchTeamPunches calls per each team to scan
        final int punchesScans = MainApp.mStation.getNumber() / (StationAPI.MAX_PUNCH_COUNT - 1) + 1;
        // Estimate remaining time in seconds
        final int secondsToComplete = (int) ((totalTeams - currentTeam) * (1 + punchesScans) * 150 / 1000L);
        // Update progress bar and text message
        ((ProgressBar) findViewById(R.id.cp_scan_percents)).setProgress(percents);
        ((TextView) findViewById(R.id.cp_scan_time)).setText(getResources()
                .getQuantityString(R.plurals.cp_scan_time, secondsToComplete,
                        secondsToComplete));
    }

    /**
     * Update layout according to activity state.
     */
    private void updateLayout() {
        // Update number of teams punched at this control point
        // (station clock will be updated in background thread)
        ((TextView) findViewById(R.id.cp_total_teams)).setText(getResources()
                .getString(R.string.cp_total_teams, MainApp.mPointPunches.size()));
        // Do nothing if full scan progress bar is visible
        if (mLongScan) return;
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
        // Show all updated elements
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
