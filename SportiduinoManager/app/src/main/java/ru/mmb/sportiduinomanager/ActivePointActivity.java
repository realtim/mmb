package ru.mmb.sportiduinomanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.sportiduinomanager.model.Chips;
import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;

import static ru.mmb.sportiduinomanager.StationPoolingService.NOTIFICATION_ID;

/**
 * Provides ability to get chip events from station, mark team members as absent,
 * update team members mask in a chip and save this data in local database.
 */
public final class ActivePointActivity extends MainActivity
        implements TeamListAdapter.OnTeamClicked, MemberListAdapter.OnMemberClicked {
    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;

    /**
     * Station which was previously paired via Bluetooth.
     */
    private Station mStation;
    /**
     * Local copy of distance (downloaded from site or loaded from local database).
     */
    private Distance mDistance;

    /**
     * User modified team members mask (it can be saved to chip and to local db).
     */
    private int mTeamMask;
    /**
     * Original team mask received from chip at last visit
     * or changed and saved previously.
     */
    private int mOriginalMask;

    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mMemberAdapter;

    /**
     * RecyclerView with list of teams visited the station.
     */
    private TeamListAdapter mTeamAdapter;

    /**
     * Receiver for messages from station pooling service.
     */
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            onStationDataUpdated();
        }
    };

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) getApplication();
        mStation = mMainApplication.getStation();
        setContentView(R.layout.activity_activepoint);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(NOTIFICATION_ID));
        // Start background querying of connected station
        runStationQuerying();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStationQuerying();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    /**
     * Create filtered chip events for current station and point.
     *
     * @return List of chips visited the point with number of connected station
     */
    @Nullable
    private Chips getFlash() {
        // Get all chips events from main application thread
        final Chips chips = mMainApplication.getChips();
        // Return events registered and connected station point number
        if (chips == null || mStation == null) {
            return null;
        } else {
            return chips.getChipsAtPoint(mStation.getNumber(), mStation.getMACasLong());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.active_point).setChecked(true);
        updateMenuItems(mMainApplication, R.id.active_point);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Get chip events from current point
        final Chips flash = getFlash();
        if (flash == null) return;
        // Get distance
        mDistance = mMainApplication.getDistance();
        // Initialize masks
        mTeamMask = mMainApplication.getTeamMask();
        mOriginalMask = 0;
        // Prepare recycler view of members list
        final RecyclerView membersList = findViewById(R.id.ap_member_list);
        final RecyclerView.LayoutManager membersLM = new LinearLayoutManager(this);
        membersList.setLayoutManager(membersLM);
        // specify an RecyclerView adapter and initialize it
        mMemberAdapter = new MemberListAdapter(this,
                ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()),
                ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        membersList.setAdapter(mMemberAdapter);
        // Prepare recycler view of team list
        final Teams teams = mMainApplication.getTeams();
        final RecyclerView teamsList = findViewById(R.id.ap_team_list);
        final RecyclerView.LayoutManager teamsLM = new LinearLayoutManager(this);
        teamsList.setLayoutManager(teamsLM);
        // Specify an RecyclerView adapter and initialize it
        mTeamAdapter = new TeamListAdapter(this, teams, flash);
        teamsList.setAdapter(mTeamAdapter);
        // Restore team list position and update activity layout
        int restoredPosition = mMainApplication.getTeamListPosition();
        if (restoredPosition > flash.size()) {
            restoredPosition = flash.size();
            mMainApplication.setTeamListPosition(restoredPosition);
        }
        updateMasks(true, restoredPosition);
        mTeamAdapter.setPosition(restoredPosition);
        updateLayout();
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
        mMainApplication.setTeamListPosition(position);
        mMainApplication.setTeamMask(mTeamMask);
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
            Toast.makeText(mMainApplication,
                    R.string.err_init_empty_team, Toast.LENGTH_LONG).show();
            mMemberAdapter.notifyItemChanged(position);
            return;
        }
        mTeamMask = newMask;
        // Save it in main application
        mMainApplication.setTeamMask(mTeamMask);
        // Update list item
        mMemberAdapter.setMask(mTeamMask);
        mMemberAdapter.notifyItemChanged(position);
        // Display new team members count
        final int teamMembersCount = Teams.getMembersCount(mTeamMask);
        ((TextView) findViewById(R.id.ap_members_count)).setText(getResources()
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
        // Create filtered chip events for current station and point
        final Chips flash = getFlash();
        // Check team mask and station presence
        if (mTeamMask == 0 || mStation == null || flash == null || flash.size() == 0) {
            Toast.makeText(mMainApplication,
                    R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Get team number
        final int index = flash.size() - 1 - mTeamAdapter.getPosition();
        final int teamNumber = flash.getTeamNumber(index);
        //Get chips events from main application thread
        final Chips chips = mMainApplication.getChips();
        // Add new event to global list with same point time and new mask
        if (!chips.updateTeamMask(teamNumber, mTeamMask, mStation, mMainApplication.getDatabase(),
                false)) {
            Toast.makeText(mMainApplication,
                    R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        mMainApplication.setChips(chips, false);
        // Replace event in copy of station memory
        if (!flash.updateTeamMask(teamNumber, mTeamMask, mStation, mMainApplication.getDatabase(),
                true)) {
            Toast.makeText(mMainApplication,
                    R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Send command to station
        stopStationQuerying();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!mStation.updateTeamMask(teamNumber, flash.getInitTime(index), mTeamMask)) {
            Toast.makeText(mMainApplication,
                    mStation.getLastError(true), Toast.LENGTH_LONG).show();
            runStationQuerying();
            return;
        }
        runStationQuerying();
        // Rebuild masks class members
        updateMasks(false, mTeamAdapter.getPosition());
        // Disable button after successful saving of new mask
        updateMaskButton();
        // Get teams from main application thread
        final Teams teams = mMainApplication.getTeams();
        // Update list of team members and their selection
        final List<String> teamMembers = teams.getMembersNames(teamNumber);
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
        final Chips flash = getFlash();
        if (flash == null) return;
        // Get the original mask of selected team
        final int index = flash.size() - 1 - position;
        mOriginalMask = flash.getTeamMask(index);
        if (restore) {
            // Restore current mask from main application
            mTeamMask = mMainApplication.getTeamMask();
            // Set it to original if the mask in main application was not initialized
            if (mTeamMask == 0) {
                mTeamMask = mOriginalMask;
                mMainApplication.setTeamMask(mTeamMask);
            }
        } else {
            // Set current mask equal to original from database
            mTeamMask = mOriginalMask;
            mMainApplication.setTeamMask(mTeamMask);
        }
    }

    /**
     * Update layout according to activity state.
     */
    private void updateLayout() {
        // Get chips from main application thread
        final Chips flash = getFlash();
        if (flash == null) return;
        // Update number of teams visited this active point
        // (station clock will be updated in background thread)
        ((TextView) findViewById(R.id.ap_total_teams)).setText(getResources()
                .getString(R.string.ap_total_teams, flash.size()));
        // Hide last team block when no teams have been visited the station
        if (flash.size() == 0) {
            findViewById(R.id.ap_team_data).setVisibility(View.GONE);
            return;
        }
        // Get index in mFlash team visits list to display
        final int index = flash.size() - 1 - mTeamAdapter.getPosition();
        // Update team number and name
        final int teamNumber = flash.getTeamNumber(index);
        final Teams teams = mMainApplication.getTeams();
        String teamName;
        if (teams == null) {
            teamName = "";
        } else {
            teamName = teams.getTeamName(teamNumber);
        }
        ((TextView) findViewById(R.id.ap_team_name)).setText(getResources()
                .getString(R.string.ap_team_name, teamNumber, teamName));
        // Update team time
        ((TextView) findViewById(R.id.ap_team_time)).setText(
                Chips.printTime(flash.getTeamTime(index), "dd.MM  HH:mm:ss"));
        // Update lists of visited points
        if (mDistance == null) {
            // TODO: show simplified list of visited points
            findViewById(R.id.ap_visited).setVisibility(View.GONE);
            findViewById(R.id.ap_skipped).setVisibility(View.GONE);
            findViewById(R.id.ap_save_mask).setVisibility(View.GONE);
            return;
        }
        final Chips chips = mMainApplication.getChips();
        final List<Integer> visited = chips.getChipMarks(teamNumber, flash.getInitTime(index),
                mStation.getNumber(), mStation.getMACasLong(), 255);
        ((TextView) findViewById(R.id.ap_visited)).setText(getResources()
                .getString(R.string.ap_visited, mDistance.pointsNamesFromList(visited)));
        // Update lists of skipped points
        final List<Integer> skipped = mDistance.getSkippedPoints(visited);
        final TextView skippedText = findViewById(R.id.ap_skipped);
        skippedText.setText(getResources().getString(R.string.ap_skipped,
                mDistance.pointsNamesFromList(skipped)));
        if (mDistance.mandatoryPointSkipped(skipped)) {
            skippedText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        } else {
            skippedText.setTextColor(ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()));
        }
        // Update saveTeamMask button state
        updateMaskButton();
        // Get list of team members
        List<String> teamMembers = new ArrayList<>();
        if (teams != null) teamMembers = teams.getMembersNames(teamNumber);
        // Update list of team members and their selection
        mMemberAdapter.updateList(teamMembers, mOriginalMask, mTeamMask);
        // Update number of team members
        ((TextView) findViewById(R.id.ap_members_count)).setText(getResources()
                .getString(R.string.team_members_count, Teams.getMembersCount(mTeamMask)));
        // Show last team block
        findViewById(R.id.ap_team_data).setVisibility(View.VISIBLE);
    }

    /**
     * Enable "Register dismiss" button if team mask was changed by user.
     */
    private void updateMaskButton() {
        final Button saveMaskButton = findViewById(R.id.ap_save_mask);
        if (mTeamMask == mOriginalMask) {
            saveMaskButton.setAlpha(MainApplication.DISABLED_BUTTON);
            saveMaskButton.setClickable(false);
        } else {
            saveMaskButton.setAlpha(MainApplication.ENABLED_BUTTON);
            saveMaskButton.setClickable(true);
        }
    }

    /**
     * Update layout with new data received from connected station.
     */
    private void onStationDataUpdated() {
        final Chips flash = getFlash();
        if (flash == null) return;
        // Save currently selected team
        final int selectedTeamN = flash.getTeamNumber(flash.size() - 1
                - mTeamAdapter.getPosition());
        // Update station clock in UI
        ((TextView) findViewById(R.id.station_clock)).setText(
                Chips.printTime(mStation.getStationTime(), "dd.MM  HH:mm:ss"));
        // Update layout if new data has been arrived and/or error has been occurred
        if (mTeamAdapter.getPosition() == 0) {
            // Reset current mask if we at first item of team list
            // as it is replaced with new team just arrived
            updateMasks(false, 0);
        } else {
            // Change position in the list to keep current team selected
            int newPosition = 0;
            for (int i = 0; i < flash.size(); i++) {
                if (flash.getTeamNumber(i) == selectedTeamN) {
                    newPosition = flash.size() - i - 1;
                    break;
                }
            }
            mTeamAdapter.setPosition(newPosition);
            mMainApplication.setTeamListPosition(newPosition);
        }
        // Update team list as we have a new team in it
        mTeamAdapter.notifyDataSetChanged();
        // Update activity layout as some elements has been changed
        updateLayout();
        // Menu should be changed if we have new event unsent to site
        updateMenuItems(mMainApplication, R.id.active_point);
    }

    /**
     * Background thread for periodic querying of connected station.
     */
    private void runStationQuerying() {
        startService(new Intent(this, StationPoolingService.class));
    }

    /**
     * Stops rescheduling of periodic station query.
     */
    private void stopStationQuerying() {
        stopService(new Intent(this, StationPoolingService.class));
    }
}
