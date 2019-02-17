package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import ru.mmb.sportiduinomanager.model.Distance;

/**
 * Provides ability to select a team, mark team members as absent,
 * init a chip for the team and save this data in local database.
 */
public class ChipInitActivity extends MainActivity {
    /**
     * Max number of symbols in mTeamNumber string.
     */
    private static final int TEAM_MEMBER_LEN = 4;

    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;

    /**
     * Team number as a string entered by user.
     */
    private String mTeamNumber;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) this.getApplication();
        mTeamNumber = "";
        setContentView(R.layout.activity_chipinit);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_init).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Update screen layout
        updateKeyboardState();
        updateTeamName();
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
        // Update team number on screen
        updateTeamName();
    }

    /**
     * Enable/disable virtual keyboard buttons
     * according to number of symbols in mTeamNumber.
     */
    private void updateKeyboardState() {
        if (mTeamNumber.length() < TEAM_MEMBER_LEN) {
            findViewById(R.id.key_1).setEnabled(true);
            findViewById(R.id.key_2).setEnabled(true);
            findViewById(R.id.key_3).setEnabled(true);
            findViewById(R.id.key_4).setEnabled(true);
            findViewById(R.id.key_5).setEnabled(true);
            findViewById(R.id.key_6).setEnabled(true);
            findViewById(R.id.key_7).setEnabled(true);
            findViewById(R.id.key_8).setEnabled(true);
            findViewById(R.id.key_9).setEnabled(true);
        } else {
            findViewById(R.id.key_1).setEnabled(false);
            findViewById(R.id.key_2).setEnabled(false);
            findViewById(R.id.key_3).setEnabled(false);
            findViewById(R.id.key_4).setEnabled(false);
            findViewById(R.id.key_5).setEnabled(false);
            findViewById(R.id.key_6).setEnabled(false);
            findViewById(R.id.key_7).setEnabled(false);
            findViewById(R.id.key_8).setEnabled(false);
            findViewById(R.id.key_9).setEnabled(false);
        }
        if (mTeamNumber.length() > 0 && mTeamNumber.length() < TEAM_MEMBER_LEN) {
            findViewById(R.id.key_0).setEnabled(true);
        } else {
            findViewById(R.id.key_0).setEnabled(false);
        }
        if (mTeamNumber.length() > 0) {
            findViewById(R.id.key_del).setEnabled(true);
            findViewById(R.id.key_clear).setEnabled(true);
        } else {
            findViewById(R.id.key_del).setEnabled(false);
            findViewById(R.id.key_clear).setEnabled(false);
        }
    }

    /**
     * Find team with entered number and update its name.
     */
    private void updateTeamName() {
        // TODO: add number of maps, number of members, list of members and Init button
        final TextView teamNumberText = findViewById(R.id.init_team_number);
        final TextView teamNameText = findViewById(R.id.init_team_name);
        final TextView errorText = findViewById(R.id.init_error);
        // Hide all if no number was entered yet
        if (mTeamNumber.length() == 0) {
            teamNumberText.setText(getResources().getString(R.string.team_number));
            teamNameText.setVisibility(View.GONE);
            errorText.setVisibility(View.GONE);
            return;
        }
        // Update team number on screen
        teamNumberText.setText(mTeamNumber);
        // Check if local database was loaded
        final Distance distance = mMainApplication.getDistance();
        if (distance == null) {
            teamNameText.setVisibility(View.GONE);
            errorText.setText(getResources().getString(R.string.err_db_no_distance_loaded));
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        // Try to find team with entered number in local database
        final int teamNumber = Integer.parseInt(mTeamNumber);
        final String teamName = distance.getTeamName(teamNumber);
        if (teamName == null) {
            teamNameText.setVisibility(View.GONE);
            errorText.setText(getResources().getString(R.string.err_init_no_such_team));
            errorText.setVisibility(View.VISIBLE);
            return;
        }
        teamNameText.setText(teamName);
        teamNameText.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
    }

}