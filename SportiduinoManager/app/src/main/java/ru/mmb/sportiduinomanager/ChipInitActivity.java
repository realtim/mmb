package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.support.constraint.Group;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.mmb.sportiduinomanager.model.Chips;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Provides ability to select a team, mark team members as absent,
 * init a chip for the team and save this data in local database.
 */
public final class ChipInitActivity extends MainActivity implements MemberListAdapter.OnItemClicked {
    /**
     * Max number of symbols in mTeamNumber string.
     */
    private static final int TEAM_MEMBER_LEN = 4;

    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;

    /**
     * Station which was previously paired via Bluetooth.
     */
    private Station mStation;

    /**
     * Chips events received from stations.
     */
    private Chips mChips;

    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mAdapter;

    /**
     * Team number as a string entered by user.
     */
    private String mTeamNumber;

    /**
     * Team members mask
     * (it can be modified after loading from db and before saving to chip).
     */
    private int mTeamMask;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) getApplication();
        setContentView(R.layout.activity_chipinit);
        updateMenuItems(mMainApplication, R.id.chip_init);
        // Load last entered team number and mask from main application
        mTeamNumber = Integer.toString(mMainApplication.getTeamNumber());
        if ("0".equals(mTeamNumber)) mTeamNumber = "";
        mTeamMask = mMainApplication.getTeamMask();
        // Prepare recycler view of device list
        final RecyclerView recyclerView = findViewById(R.id.member_list);
        // use a linear layout manager
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an RecyclerView adapter
        // and copy saved device list from main application
        mAdapter = new MemberListAdapter(this);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_init).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Get connected station from main application thread
        mStation = mMainApplication.getStation();
        //Get chips events from main application thread
        mChips = mMainApplication.getChips();
        // Update screen layout
        updateKeyboardState();
        loadTeam(false);
    }

    /**
     * The onClick implementation of the RecyclerView item click.
     */
    @Override
    public void onItemClick(final int position) {
        // Update team mask
        mTeamMask = mTeamMask ^ (1 << position);
        // Save it to main application
        mMainApplication.setTeamMask(mTeamMask);
        // Display new team members count
        final int teamMembersCount = getMembersCount();
        ((TextView) findViewById(R.id.init_members_count)).setText(getResources()
                .getString(R.string.team_members_count, teamMembersCount,
                        Integer.toString(mTeamMask, 2)));
        // Hide init chip button if user has deselected all team members
        if (teamMembersCount == 0) {
            showError(false, R.string.err_init_empty_team);
        } else {
            findViewById(R.id.init_error).setVisibility(View.INVISIBLE);
            findViewById(R.id.init_team_chip).setVisibility(View.VISIBLE);
        }
    }

    /**
     * Process app virtual keyboard button click.
     *
     * @param view View of button clicked
     */
    public void keyboardButtonClicked(final View view) {
        // Update team number string according to button clicked
        switch (view.getId()) {
            case R.id.key_0:
                mTeamNumber += "0";
                break;
            case R.id.key_1:
                mTeamNumber += "1";
                break;
            case R.id.key_2:
                mTeamNumber += "2";
                break;
            case R.id.key_3:
                mTeamNumber += "3";
                break;
            case R.id.key_4:
                mTeamNumber += "4";
                break;
            case R.id.key_5:
                mTeamNumber += "5";
                break;
            case R.id.key_6:
                mTeamNumber += "6";
                break;
            case R.id.key_7:
                mTeamNumber += "7";
                break;
            case R.id.key_8:
                mTeamNumber += "8";
                break;
            case R.id.key_9:
                mTeamNumber += "9";
                break;
            case R.id.key_del:
                if (mTeamNumber.length() > 0) {
                    mTeamNumber = mTeamNumber.substring(0, mTeamNumber.length() - 1);
                }
                break;
            case R.id.key_clear:
                mTeamNumber = "";
                break;
            default:
                return;
        }
        // Enable/disable buttons after mTeamNumber length change
        updateKeyboardState();
        // Load team with entered number
        loadTeam(true);
    }

    /**
     * Init chip for the selected team.
     *
     * @param view View of button clicked
     */
    public void initChip(final View view) {
        // Check team number, mask and station presence
        if (mTeamNumber.length() == 0 || mTeamMask == 0 || mStation == null) return;
        final int teamNumber = Integer.parseInt(mTeamNumber);
        // Send command to station and check result
        if (mStation.initChip(teamNumber, mTeamMask)) {
            // Create new chip init event and save it into local database
            mChips.addNewEvent(mStation, mStation.getLastInitTime(), teamNumber, mTeamMask,
                    mStation.getNumber(), mStation.getLastInitTime());
            final String result = mChips.saveNewEvents(mMainApplication.getDatabase());
            if ("".equals(result)) {
                // Clear team number and mask to start again
                mTeamNumber = "";
                mTeamMask = 0;
                // Update onscreen keyboard and "load" empty team
                updateKeyboardState();
                loadTeam(true);
                Toast.makeText(getApplicationContext(), getString(R.string.response_time,
                        mStation.getResponseTime()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
            }
            // Copy changed list of chips events to main application
            mMainApplication.setChips(mChips, false);
        } else {
            Toast.makeText(getApplicationContext(), mStation.getLastError(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Enable/disable virtual keyboard buttons
     * according to number of symbols in mTeamNumber.
     */
    private void updateKeyboardState() {
        if (mTeamNumber.length() < TEAM_MEMBER_LEN) {
            changeKeyState(R.id.key_1, true);
            changeKeyState(R.id.key_2, true);
            changeKeyState(R.id.key_3, true);
            changeKeyState(R.id.key_4, true);
            changeKeyState(R.id.key_5, true);
            changeKeyState(R.id.key_6, true);
            changeKeyState(R.id.key_7, true);
            changeKeyState(R.id.key_8, true);
            changeKeyState(R.id.key_9, true);
        } else {
            changeKeyState(R.id.key_1, false);
            changeKeyState(R.id.key_2, false);
            changeKeyState(R.id.key_3, false);
            changeKeyState(R.id.key_4, false);
            changeKeyState(R.id.key_5, false);
            changeKeyState(R.id.key_6, false);
            changeKeyState(R.id.key_7, false);
            changeKeyState(R.id.key_8, false);
            changeKeyState(R.id.key_9, false);
        }
        changeKeyState(R.id.key_0, mTeamNumber.length() > 0 && mTeamNumber.length() < TEAM_MEMBER_LEN);
        if (mTeamNumber.length() > 0) {
            changeKeyState(R.id.key_del, true);
            changeKeyState(R.id.key_clear, true);
        } else {
            changeKeyState(R.id.key_del, false);
            changeKeyState(R.id.key_clear, false);
        }
    }

    /**
     * Change key button state and appearance.
     *
     * @param buttonId  Button resource id
     * @param isEnabled true for enabled button
     */
    private void changeKeyState(final int buttonId, final boolean isEnabled) {
        final Button getResultsButton = findViewById(buttonId);
        if (isEnabled) {
            getResultsButton.setAlpha(MainApplication.ENABLED_BUTTON);
            getResultsButton.setClickable(true);
        } else {
            getResultsButton.setAlpha(MainApplication.DISABLED_BUTTON);
            getResultsButton.setClickable(false);
        }
    }

    /**
     * Try to find team with entered number and update layout with it's data.
     *
     * @param isNew True if the team was selected right now,
     *              False if it is being reloaded after activity restart.
     */
    private void loadTeam(final boolean isNew) {
        // Parse integer team number from string
        int teamNumber;
        if (mTeamNumber.length() > 0) {
            teamNumber = Integer.parseInt(mTeamNumber);
        } else {
            teamNumber = 0;
        }
        // Save it in main application for new team
        if (isNew) {
            mMainApplication.setTeamNumber(teamNumber);
        }
        // No errors found yet
        findViewById(R.id.init_error).setVisibility(View.INVISIBLE);
        // Check if we can send initialization command to a station
        if (mStation == null) {
            showError(true, R.string.err_init_no_station);
            return;
        }
        if (mStation.getMode() != Station.MODE_INIT_CHIPS) {
            showError(true, R.string.err_init_wrong_mode);
            return;
        }
        // Save layout elements views for future usage
        final Button initButton = findViewById(R.id.init_team_chip);
        final TextView teamNumberText = findViewById(R.id.init_team_number);
        final Group teamData = findViewById(R.id.init_team_data);
        // Hide all if no number was entered yet
        if (teamNumber == 0) {
            teamNumberText.setText(getResources().getString(R.string.team_number));
            initButton.setVisibility(View.GONE);
            teamData.setVisibility(View.GONE);
            return;
        }
        // Update team number on screen
        teamNumberText.setText(mTeamNumber);
        // Check if local database was loaded
        final Teams teams = mMainApplication.getTeams();
        if (teams == null) {
            showError(true, R.string.err_db_no_distance_loaded);
            return;
        }
        // Try to find team with entered number in local database
        final String teamName = teams.getTeamName(teamNumber);
        if (teamName == null) {
            showError(true, R.string.err_init_no_such_team);
            return;
        }
        // TODO: check if we already initialized a chip for this team
        // Update team name and map count
        ((TextView) findViewById(R.id.init_team_name)).setText(teamName);
        ((TextView) findViewById(R.id.init_team_maps)).setText(getResources()
                .getString(R.string.team_maps_count, teams.getTeamMaps(teamNumber)));
        // Get list of team members
        final List<String> teamMembers = teams.getMembersNames(teamNumber);
        // Mark all members as selected for new team,
        // leave mask untouched for old team being reloaded
        if (isNew) {
            mTeamMask = 0;
            for (int i = 0; i < teamMembers.size(); i++) {
                mTeamMask = mTeamMask | (1 << i);
            }
            mMainApplication.setTeamMask(mTeamMask);
        }
        // Update team members list
        mAdapter.fillList(teamMembers, mTeamMask);
        // Update number of members in the team
        ((TextView) findViewById(R.id.init_members_count)).setText(getResources()
                .getString(R.string.team_members_count, getMembersCount(),
                        Integer.toString(mTeamMask, 2)));
        // Check if some team members were selected as present
        if (mTeamMask == 0) {
            showError(false, R.string.err_init_empty_team);
            return;
        }
        // Make all team data visible
        initButton.setVisibility(View.VISIBLE);
        teamData.setVisibility(View.VISIBLE);
    }

    /**
     * Show error message instead of Chip init button.
     *
     * @param teamNotFound True for errors connected with team absence
     * @param errorId      Message resource id
     */
    private void showError(final boolean teamNotFound, final int errorId) {
        final Button initButton = findViewById(R.id.init_team_chip);
        final Group teamData = findViewById(R.id.init_team_data);
        final TextView errorText = findViewById(R.id.init_error);
        if (teamNotFound) {
            initButton.setVisibility(View.GONE);
            teamData.setVisibility(View.GONE);
        } else {
            initButton.setVisibility(View.INVISIBLE);
        }
        errorText.setText(getResources().getString(errorId));
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Get current count of team members.
     *
     * @return Number of team members
     */
    private int getMembersCount() {
        int teamMembersCount = 0;
        for (int i = 0; i < 16; i++) {
            if ((mTeamMask & (1 << i)) != 0) {
                teamMembersCount++;
            }
        }
        return teamMembersCount;
    }

}
