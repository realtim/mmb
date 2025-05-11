package ru.mmb.sportiduinomanager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.mmb.sportiduinomanager.adapter.MemberListAdapter;
import ru.mmb.sportiduinomanager.adapter.PointListAdapter;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.task.ChipInfoTask;

/**
 * Read chip info and display it.
 */
public final class ChipInfoActivity extends MenuActivity {
    /**
     * Chip info request not running now.
     */
    public static final int INFO_REQUEST_OFF = 0;
    /**
     * Chip info request is in progress.
     */
    public static final int INFO_REQUEST_ON = 1;

    /**
     * Current state of chip info request.
     */
    private int mInfoRequest;
    /**
     * RecyclerView with team members.
     */
    private MemberListAdapter mMemberAdapter;
    /**
     * RecyclerView with point punches.
     */
    private PointListAdapter mPointAdapter;

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        setContentView(R.layout.activity_chipinfo);
        updateMenuItems(R.id.chip_info);
        mInfoRequest = INFO_REQUEST_OFF;
    }

    @Override
    protected void onPause() {
        // Save SaveToDB switch state
        MainApp.UI_STATE.setChipInfoSaveToDB(((Switch) findViewById(R.id.info_save_to_db)).isChecked());
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.chip_info).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Prepare recycler view of members list
        final RecyclerView membersList = findViewById(R.id.info_member_list);
        final RecyclerView.LayoutManager membersLM = new LinearLayoutManager(this);
        membersList.setLayoutManager(membersLM);
        // specify team members adapter and initialize it
        mMemberAdapter = new MemberListAdapter(null,
                ResourcesCompat.getColor(getResources(), R.color.text_secondary, getTheme()),
                ResourcesCompat.getColor(getResources(), R.color.bg_secondary, getTheme()));
        membersList.setAdapter(mMemberAdapter);
        // Prepare recycler view of points list
        final RecyclerView pointsList = findViewById(R.id.info_point_list);
        final RecyclerView.LayoutManager pointsLM = new LinearLayoutManager(this);
        pointsList.setLayoutManager(pointsLM);
        // specify points list adapter and initialize it
        mPointAdapter = new PointListAdapter(MainApp.mChipPunches);
        pointsList.setAdapter(mPointAdapter);
        // Restore SaveToDB switch state
        ((Switch) findViewById(R.id.info_save_to_db)).setChecked(MainApp.UI_STATE.getChipInfoSaveToDB());
        // Update layout
        updateLayout();
    }

    /**
     * Accessor method to change mInfoRequest from AsyncTask.
     *
     * @param newState new state INFO_REQUEST_ON or INFO_REQUEST_OFF
     */
    public void setInfoRequestState(final int newState) {
        mInfoRequest = newState;
    }

    /**
     * Read chip info.
     *
     * @param view View of button clicked (unused)
     */
    public void readChipInfo(@SuppressWarnings("unused") final View view) {
        // Save SaveToDB switch state
        final boolean saveToDB = ((Switch) findViewById(R.id.info_save_to_db)).isChecked();
        MainApp.UI_STATE.setChipInfoSaveToDB(saveToDB);
        // Check station presence
        if (MainApp.mStation == null) return;
        // Send command to station and check result
        new ChipInfoTask(this).execute(saveToDB);
    }

    /**
     * Refresh UI controls state.
     */
    public void updateLayout() {
        final Button infoButton = findViewById(R.id.info_request_button);
        final ProgressBar infoProgress = findViewById(R.id.info_request_progress);
        final TextView infoHelp = findViewById(R.id.info_help);
        final View infoTeamData = findViewById(R.id.info_team_data);
        // Show request progress if request to station is being processed
        if (mInfoRequest == INFO_REQUEST_ON) {
            infoButton.setVisibility(View.INVISIBLE);
            infoProgress.setVisibility(View.VISIBLE);
            infoHelp.setVisibility(View.GONE);
            infoTeamData.setVisibility(View.GONE);
            return;
        }
        // Hide request progress, it was finished or has not started yet
        infoProgress.setVisibility(View.INVISIBLE);
        infoButton.setVisibility(View.VISIBLE);
        // Show help only if request failed or has not been sent yet
        if (MainApp.mChipPunches.isEmpty()) {
            infoTeamData.setVisibility(View.GONE);
            infoHelp.setVisibility(View.VISIBLE);
            return;
        }
        infoHelp.setVisibility(View.GONE);
        // Display team number and chip init time
        final int teamNumber = MainApp.mChipPunches.getTeamNumber(0);
        final String teamName = MainApp.mTeams.getTeamName(teamNumber);
        ((TextView) findViewById(R.id.info_team_name)).setText(getResources()
                .getString(R.string.info_team_name, teamNumber, teamName));
        final long initTime = MainApp.mChipPunches.getInitTime(0);
        ((TextView) findViewById(R.id.info_init_time)).setText(Records.printTime(initTime, "dd.MM.yyyy  HH:mm:ss"));
        // Display team members
        final int teamMask = MainApp.mChipPunches.getTeamMask(0);
        // Prepare list of team members
        final List<String> teamMembers = MainApp.mTeams.getMembersNames(teamNumber);
        mMemberAdapter.updateList(teamMembers, teamMask, teamMask);
        // Prepare list of punches at control points
        mPointAdapter.updateList(MainApp.mChipPunches);
        // Show updated chip info
        infoTeamData.setVisibility(View.VISIBLE);
        // Menu should be updated as we may have new records unsent to site
        updateMenuItems(R.id.chip_info);
    }
}
