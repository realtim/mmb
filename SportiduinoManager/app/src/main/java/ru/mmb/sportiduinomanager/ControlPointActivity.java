package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Provides ability to get Sportiduino records from station, mark team members
 * as absent, update team members mask in a chip and save this data
 * in local database.
 */
public final class ControlPointActivity extends MainActivity
        implements TeamListAdapter.OnTeamClicked, MemberListAdapter.OnMemberClicked {
    /**
     * Main application thread with persistent data.
     */
    private MainApp mMainApplication;
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
     * Timer of background thread for communication with the station.
     */
    private Timer mTimer;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApp) getApplication();
        setContentView(R.layout.activity_controlpoint);
    }

    @Override
    protected void onStart() {
        Log.d(StationAPI.CALLER_CP, "Start");
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.control_point).setChecked(true);
        updateMenuItems(R.id.control_point);
        // Disable startup animation
        overridePendingTransition(0, 0);

        // Initialize masks
        mTeamMask = mMainApplication.getTeamMask();
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
        int restoredPosition = mMainApplication.getTeamListPosition();
        if (restoredPosition > MainApp.mPointPunches.size()) {
            restoredPosition = MainApp.mPointPunches.size();
            mMainApplication.setTeamListPosition(restoredPosition);
        }
        updateMasks(true, restoredPosition);
        mTeamAdapter.setPosition(restoredPosition);
        updateLayout();
    }

    @Override
    protected void onResume() {
        Log.d(StationAPI.CALLER_CP, "Resume");
        super.onResume();
        // Start background querying of connected station
        runStationQuerying();
    }

    @Override
    protected void onPause() {
        stopStationQuerying();
        Log.d(StationAPI.CALLER_CP, "Pause");
        super.onPause();
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
        if (mTeamMask == 0 || MainApp.mStation == null || MainApp.mPointPunches.size() == 0) {
            Toast.makeText(mMainApplication,
                    R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Get team number
        final int index = MainApp.mPointPunches.size() - 1 - mTeamAdapter.getPosition();
        final int teamNumber = MainApp.mPointPunches.getTeamNumber(index);
        // Add new record to global list with same point time and new mask
        if (!MainApp.mAllRecords.updateTeamMask(teamNumber, mTeamMask, MainApp.mStation,
                MainApp.mDatabase, false)) {
            Toast.makeText(mMainApplication,
                    R.string.err_internal_error, Toast.LENGTH_LONG).show();
            return;
        }
        // Replace punch with same record with new mask
        if (!MainApp.mPointPunches.updateTeamMask(teamNumber, mTeamMask, MainApp.mStation,
                MainApp.mDatabase, true)) {
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
        MainApp.mStation.waitForQuerying2Stop();
        if (!MainApp.mStation.updateTeamMask(teamNumber, MainApp.mPointPunches.getInitTime(index),
                mTeamMask, StationAPI.CALLER_CP)) {
            Toast.makeText(mMainApplication, MainApp.mStation.getLastError(true),
                    Toast.LENGTH_LONG).show();
            runStationQuerying();
            return;
        }
        runStationQuerying();
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
        // Update number of teams punched at this control point
        // (station clock will be updated in background thread)
        ((TextView) findViewById(R.id.cp_total_teams)).setText(getResources()
                .getString(R.string.cp_total_teams, MainApp.mPointPunches.size()));
        // Hide last team block when no teams have been punched at the station
        if (MainApp.mPointPunches.size() == 0) {
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
        // Do nothing if no teams has been punched yet
        if (MainApp.mStation.getTeamsPunched() == 0) return 0;
        // Number of team punches at local db and at station are the same?
        // Time of last punch in local db and at station is the same?
        // (it can change without changing of number of teams)
        if (MainApp.mPointPunches.size() == MainApp.mStation.getTeamsPunched()
                && MainApp.mPointPunches.getTeamTime(MainApp.mPointPunches.size() - 1)
                == MainApp.mStation.getLastPunchTime()) {
            return 0;
        }

        // Ok, we have some new punches
        boolean fullDownload = false;
        // Clone previous teams list from station
        final List<Integer> prevLastTeams = new ArrayList<>(MainApp.mStation.getLastTeams());
        // Ask station for new list
        if (!MainApp.mStation.fetchLastTeams(StationAPI.CALLER_QUERYING)) {
            fullDownload = true;
        }
        final List<Integer> currLastTeams = MainApp.mStation.getLastTeams();
        // Check if teams from previous list were copied from flash
        for (final int team : prevLastTeams) {
            if (!MainApp.mPointPunches.contains(team, MainApp.mStation.getNumber())) {
                // Something strange has been happened, do full download of all teams
                fullDownload = true;
            }
        }
        // Start building the final list of teams to fetch
        List<Integer> fetchTeams = new ArrayList<>();
        for (final int team : currLastTeams) {
            if (!prevLastTeams.contains(team)) {
                fetchTeams.add(team);
            }
        }
        // If all members of last teams buffer are new to us,
        // then we need to make a full rescan
        if (fetchTeams.size() == StationAPI.LAST_TEAMS_LEN) {
            fullDownload = true;
        }
        // If all last teams are the same but last team time has been changed
        // then we need to rescan all teams from the buffer
        if (fetchTeams.isEmpty()) {
            fetchTeams = currLastTeams;
        }
        // For full rescan of all teams make a list of all registered teams
        if (fullDownload) {
            fetchTeams = MainApp.mTeams.getTeamList();
        }

        // Get Sportiduino records for all teams in fetch list
        boolean flashChanged = false;
        boolean newRecords = false;
        int stationError = 0;
        for (final int teamNumber : fetchTeams) {
            // Fetch data for the team punched at the station
            int newError = 0;
            if (!MainApp.mStation.fetchTeamHeader(teamNumber, StationAPI.CALLER_QUERYING)) {
                newError = MainApp.mStation.getLastError(true);
                // Ignore data absence for teams which are not in last teams list
                // Most probable these teams did not punched at the station at all
                if (newError == R.string.err_station_no_data
                        && !currLastTeams.contains(teamNumber)) {
                    continue;
                }
                // Abort scanning in case of serious error
                // Continue scanning in case of problems with copying data from chip to memory
                if (newError != R.string.err_station_flash_empty
                        && newError != R.string.err_station_no_data) {
                    return newError;
                }
            }
            // Get team punches as a Sportiduino record list
            final Records teamPunches = MainApp.mStation.getRecords();
            if (teamPunches.size() == 0) {
                // Team punch was not registered at all due to err_station_no_data error
                // Create synthetic team punch with zero chip init time
                final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
                int originalMask = 0;
                for (int i = 0; i < teamMembers.size(); i++) {
                    originalMask = originalMask | (1 << i);
                }
                teamPunches.addRecord(MainApp.mStation, 0, teamNumber, originalMask,
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
                    if (marks <= 0) break;
                    int toRead = marks;
                    if (toRead > StationAPI.MAX_PUNCH_COUNT) {
                        toRead = StationAPI.MAX_PUNCH_COUNT;
                    }
                    if (!MainApp.mStation.fetchTeamPunches(teamNumber, initTime, teamMask, fromMark, toRead,
                            StationAPI.CALLER_QUERYING)) {
                        return MainApp.mStation.getLastError(true);
                    }
                    fromMark += toRead;
                    // Add fetched punches to application list of records
                    MainApp.mAllRecords.join(MainApp.mStation.getRecords());
                } while (fromMark < marks);
            } else {
                // Ignore recurrent problem with copying data from chip to memory
                // as we already created synthetic team punch and warned a user
                newError = 0;
            }
            // Save non-fatal station error
            if (newError == R.string.err_station_flash_empty
                    || newError == R.string.err_station_no_data) {
                stationError = newError;
            }
        }

        // We have asked station for all teams from fetch list
        // Save new records (if any) to local db
        if (newRecords) {
            // Save new records in local database
            final String result = MainApp.mAllRecords.saveNewRecords(MainApp.mDatabase);
            if (!"".equals(result)) return R.string.err_db_sql_error;
        }
        // Sort punches by their time
        if (flashChanged) {
            MainApp.mPointPunches.sort();
        }
        // Report non-fatal errors which has been occurred during scanning
        if (stationError != 0) {
            return stationError;
        }
        // Report 'data changed' for updating UI
        if (newRecords || flashChanged) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Background thread for periodic querying of connected station.
     */
    private void runStationQuerying() {
        stopStationQuerying();
        MainApp.mStation.setQueryingAllowed(true);
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            /**
             * Run every 1s, get station status and get new team data (if any).
             */
            public void run() {
                // Station was not connected yet
                if (MainApp.mStation == null) {
                    stopStationQuerying();
                    return;
                }
                // Querying is scheduled to stop, return immediately
                if (!MainApp.mStation.isQueryingAllowed()) {
                    Log.d(StationAPI.CALLER_QUERYING, "Skip querying");
                    return;
                }
                // Inform other activities that service starts sending queries to station
                MainApp.mStation.setQueryingActive(true);
                // Save currently selected team
                final int selectedTeamN = MainApp.mPointPunches.getTeamNumber(MainApp.mPointPunches.size() - 1
                        - mTeamAdapter.getPosition());
                // Fetch current station status
                MainApp.mStation.fetchStatus(StationAPI.CALLER_QUERYING);
                // Get the latest data from connected station
                final int result = fetchTeamsPunches();
                // Inform other activities that service finished sending queries to station
                MainApp.mStation.setQueryingActive(false);
                // Update control point activity screen elements
                runOnUiThread(() -> {
                    // Update station clock in UI
                    ((TextView) findViewById(R.id.station_clock)).setText(
                            Records.printTime(MainApp.mStation.getStationTime(), "dd.MM  HH:mm:ss"));
                    // Update layout if new data has been arrived and/or error has been occurred
                    if (result != 0) {
                        if (mTeamAdapter.getPosition() == 0) {
                            // Reset current mask if we at first item of team list
                            // as it is replaced with new team just arrived
                            updateMasks(false, 0);
                        } else {
                            // Change position in the list to keep current team selected
                            int newPosition = 0;
                            for (int i = 0; i < MainApp.mPointPunches.size(); i++) {
                                if (MainApp.mPointPunches.getTeamNumber(i) == selectedTeamN) {
                                    newPosition = MainApp.mPointPunches.size() - i - 1;
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
                        // Menu should be changed if we have new records unsent to site
                        updateMenuItems(R.id.control_point);
                    }
                    // Display station communication error (if any)
                    if (result > 0) {
                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, 1000, 1000);
    }

    /**
     * Stops rescheduling of periodic station query.
     */
    private void stopStationQuerying() {
        MainApp.mStation.setQueryingAllowed(false);
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
    }
}
