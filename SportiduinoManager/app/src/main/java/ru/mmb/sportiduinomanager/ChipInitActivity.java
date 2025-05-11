package ru.mmb.sportiduinomanager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mmb.sportiduinomanager.adapter.MemberListAdapter;
import ru.mmb.sportiduinomanager.model.StationAPI;
import ru.mmb.sportiduinomanager.model.Teams;
import ru.mmb.sportiduinomanager.task.ChipInitTask;

/**
 * Provides ability to select a team, mark team members as absent,
 * init a chip for the team and save this data in local database.
 */
public final class ChipInitActivity extends MenuActivity implements MemberListAdapter.OnMemberClicked {
    /**
     * Chip init not running now.
     */
    private static final int CHIP_INIT_OFF = 0;
    /**
     * Chip init is in progress.
     */
    private static final int CHIP_INIT_ON = 1;
    /**
     * Max number of symbols in mTeamNumber string.
     */
    private static final int TEAM_MEMBER_LEN = 4;

    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mAdapter;

    /**
     * Team number as a string entered by user.
     */
    private String mTeamNumber;

    /**
     * User modified team members mask (it can be saved to chip and to local db).
     */
    private int mTeamMask;

    /**
     * Current state of chip init.
     */
    private int mChipInit = CHIP_INIT_OFF;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        setContentView(R.layout.activity_chipinit);
        // Load last entered team number and mask from main application
        mTeamNumber = Integer.toString(MainApp.UI_STATE.getTeamNumber());
        if ("0".equals(mTeamNumber)) mTeamNumber = "";
        mTeamMask = MainApp.UI_STATE.getTeamMask();
        // Prepare recycler view of members list
        final RecyclerView recyclerView = findViewById(R.id.member_list);
        // use a linear layout manager
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an RecyclerView adapter
        // and copy saved device list from main application
        mAdapter = new MemberListAdapter(this,
                ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()),
                ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_init).setChecked(true);
        updateMenuItems(R.id.chip_init);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Update screen layout
        updateKeyboardState();
        loadTeam(false);
    }

    /**
     * The onClick implementation of the RecyclerView team member click.
     */
    @Override
    public void onMemberClick(final int position) {
        // Update team mask
        final int newMask = mTeamMask ^ (1 << position);
        if (newMask == 0) {
            Toast.makeText(this, R.string.err_init_empty_team, Toast.LENGTH_LONG).show();
            mAdapter.notifyItemChanged(position);
            return;
        }
        mTeamMask = newMask;
        // Save it to main application
        MainApp.UI_STATE.setTeamMask(mTeamMask);
        // Update list item
        mAdapter.setMask(mTeamMask);
        mAdapter.notifyItemChanged(position);
        // Display new team members count
        final int teamMembersCount = Teams.getMembersCount(mTeamMask);
        ((TextView) findViewById(R.id.init_members_count)).setText(getResources()
                .getString(R.string.team_members_count, teamMembersCount));
    }

    /**
     * Process app virtual keyboard button click.
     *
     * @param view View of button clicked
     */
    @SuppressLint("NonConstantResourceId")
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
                if (!mTeamNumber.isEmpty()) {
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
     * @param view View of button clicked (unused)
     */
    public void initChip(@SuppressWarnings("unused") final View view) {
        // Check team number, mask and station presence
        if (mTeamNumber.isEmpty() || mTeamMask == 0 || MainApp.mStation == null) return;
        final int teamNumber = Integer.parseInt(mTeamNumber);
        // Change chip init state
        mChipInit = CHIP_INIT_ON;
        // Update activity layout
        loadTeam(false);
        // Send command to station
        new ChipInitTask(this).execute(teamNumber, mTeamMask);
    }

    /**
     * Update team controls after chip initialization.
     *
     * @param initResult success or error
     */
    public void onChipInitResult(final boolean initResult) {
        // Change chip init state
        mChipInit = CHIP_INIT_OFF;
        if (initResult) {
            // Create new chip init record and save it into local database
            final int teamNumber = Integer.parseInt(mTeamNumber);
            MainApp.mAllRecords.addRecord(MainApp.mStation, MainApp.mStation.getMode(),
                    MainApp.mStation.getLastInitTime(), teamNumber, mTeamMask,
                    MainApp.mStation.getNumber(), MainApp.mStation.getLastInitTime());
            final String result = MainApp.mAllRecords.saveNewRecords(MainApp.mDatabase);
            if ("".equals(result)) {
                // Menu should be changed - we have new records unsent to site
                updateMenuItems(R.id.chip_init);
                // Clear team number and mask to start again
                mTeamNumber = "";
                mTeamMask = 0;
                // Update onscreen keyboard and "load" empty team
                updateKeyboardState();
                loadTeam(true);
                Toast.makeText(this, R.string.init_success, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            }
        } else {
            // Chip initialization failed
            if (MainApp.mStation == null) {
                Toast.makeText(this, R.string.err_bt_cant_connect, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, MainApp.mStation.getLastError(true), Toast.LENGTH_LONG).show();
            }
            loadTeam(false);
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
        changeKeyState(R.id.key_0, !mTeamNumber.isEmpty() && mTeamNumber.length() < TEAM_MEMBER_LEN);
        if (mTeamNumber.isEmpty()) {
            changeKeyState(R.id.key_del, false);
            changeKeyState(R.id.key_clear, false);
        } else {
            changeKeyState(R.id.key_del, true);
            changeKeyState(R.id.key_clear, true);
        }
    }

    /**
     * Change key button state and appearance.
     *
     * @param buttonId  Button resource id
     * @param isEnabled true for enabled button
     */
    private void changeKeyState(final int buttonId, final boolean isEnabled) {
        final boolean enabled = isEnabled && mChipInit == CHIP_INIT_OFF;
        final Button getResultsButton = findViewById(buttonId);
        if (enabled) {
            getResultsButton.setAlpha(MainApp.ENABLED_BUTTON);
            getResultsButton.setClickable(true);
        } else {
            getResultsButton.setAlpha(MainApp.DISABLED_BUTTON);
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
        final int teamNumber;
        if (mTeamNumber.isEmpty()) {
            teamNumber = 0;
        } else {
            teamNumber = Integer.parseInt(mTeamNumber);
        }
        // Save it in main application for new team
        if (isNew) {
            MainApp.UI_STATE.setTeamNumber(teamNumber);
        }
        // Check if we can send initialization command to a station
        if (MainApp.mStation == null) {
            showError(true, R.string.err_init_no_station);
            return;
        }
        if (MainApp.mStation.getMode() != StationAPI.MODE_INIT_CHIPS) {
            showError(true, R.string.err_init_wrong_mode);
            return;
        }
        // No errors found yet
        findViewById(R.id.init_error).setVisibility(View.INVISIBLE);
        findViewById(R.id.init_already).setVisibility(View.GONE);
        // Save layout elements views for future usage
        final Button initButton = findViewById(R.id.init_team_chip);
        final ProgressBar initProgress = findViewById(R.id.init_team_chip_progress);
        final TextView teamNumberText = findViewById(R.id.init_team_number);
        final Group teamData = findViewById(R.id.init_team_data);
        // Hide all if no number was entered yet
        if (teamNumber == 0) {
            initButton.setVisibility(View.INVISIBLE);
            initProgress.setVisibility(View.INVISIBLE);
            teamNumberText.setText(getResources().getString(R.string.team_number));
            teamData.setVisibility(View.GONE);
            return;
        }
        // Show button or progress (if chip init task is running)
        if (mChipInit == CHIP_INIT_ON) {
            initButton.setVisibility(View.INVISIBLE);
            initProgress.setVisibility(View.VISIBLE);
        } else {
            initButton.setVisibility(View.VISIBLE);
            initProgress.setVisibility(View.INVISIBLE);
        }
        // Update team number on screen
        teamNumberText.setText(mTeamNumber);
        // Try to find team with entered number in local database
        final String teamName = MainApp.mTeams.getTeamName(teamNumber);
        if (teamName == null) {
            showError(true, R.string.err_init_no_such_team);
            return;
        }
        // Show warning if a chip was already initialized for a team with this number
        // TODO: Search in mResults, not in mAllRecords
        if (MainApp.mAllRecords.contains(teamNumber, 0)) {
            findViewById(R.id.init_already).setVisibility(View.VISIBLE);
        }
        // Update team name and map count
        ((TextView) findViewById(R.id.init_team_name)).setText(teamName);
        ((TextView) findViewById(R.id.init_team_maps)).setText(getResources()
                .getString(R.string.team_maps_count, MainApp.mTeams.getTeamMaps(teamNumber)));
        // Get list of team members
        final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
        // Compute original mask (where all team members are present)
        int originalMask = 0;
        for (int i = 0; i < teamMembers.size(); i++) {
            originalMask = originalMask | (1 << i);
        }
        // Mark all members as selected for new team,
        // leave mask untouched for old team being reloaded
        if (isNew) {
            mTeamMask = originalMask;
            MainApp.UI_STATE.setTeamMask(mTeamMask);
        }
        // Update team members list
        mAdapter.updateList(teamMembers, originalMask, mTeamMask);
        // Update number of members in the team
        ((TextView) findViewById(R.id.init_members_count)).setText(getResources()
                .getString(R.string.team_members_count, Teams.getMembersCount(mTeamMask)));
        // Check if some team members were selected as present
        if (mTeamMask == 0) {
            showError(false, R.string.err_init_empty_team);
            return;
        }
        // Make all team data visible
        teamData.setVisibility(View.VISIBLE);
    }

    /**
     * Show error message instead of Chip init button.
     *
     * @param teamNotFound True for errors connected with team absence
     * @param errorId      Message resource id
     */
    private void showError(final boolean teamNotFound, final int errorId) {
        findViewById(R.id.init_team_chip).setVisibility(View.INVISIBLE);
        findViewById(R.id.init_team_chip_progress).setVisibility(View.INVISIBLE);
        findViewById(R.id.init_already).setVisibility(View.GONE);
        if (teamNotFound) {
            findViewById(R.id.init_team_data).setVisibility(View.GONE);
        }
        final TextView errorText = findViewById(R.id.init_error);
        errorText.setText(getResources().getString(errorId));
        errorText.setVisibility(View.VISIBLE);
    }

}
