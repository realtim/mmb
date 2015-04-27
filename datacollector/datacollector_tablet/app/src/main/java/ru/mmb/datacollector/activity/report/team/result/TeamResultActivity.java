package ru.mmb.datacollector.activity.report.team.result;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.report.team.FilterPanel;
import ru.mmb.datacollector.activity.report.team.FilterStateChangeListener;
import ru.mmb.datacollector.activity.report.team.result.model.DataProvider;
import ru.mmb.datacollector.activity.report.team.result.model.TeamListRecord;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_RESULT_MESSAGE;

public class TeamResultActivity extends FragmentActivity implements FilterStateChangeListener {
    private static final boolean CLEAR_SELECTED_TEAM = true;

    private TeamResultActivityState currentState;

    private TeamsAdapter adapterByNumber;
    private TeamsAdapter adapterByMember;

    private ListView lvTeams;
    private LinearLayout progressPanel;

    private FilterPanel filterPanel;
    private SortButtons sortButtons;

    private Handler buildResultFinishHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentState = new TeamResultActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.report_team_result);

        int[] filterFieldIds = {R.id.reportTeam_filterClearButton, R.id.reportTeam_filterHideButton,
                R.id.reportTeam_filterStatusTextView, R.id.reportTeam_filterNumberEdit, R.id.reportTeam_filterNumberExactCheck,
                R.id.reportTeam_filterTeamEdit, R.id.reportTeam_filterMemberEdit, R.id.reportTeam_filterNumberPanel,
                R.id.reportTeam_filterTeamAndMemberPanel};
        filterPanel = new FilterPanel(this, currentState.getFilterPanelState(), filterFieldIds);
        filterPanel.addFilterStateChangeListener(this);
        sortButtons = new SortButtons(this, currentState);

        lvTeams = (ListView) findViewById(R.id.reportTeam_teamsList);
        initListAdapters();
        lvTeams.setOnItemLongClickListener(new LvTeamsItemLongClickListener());

        progressPanel = (LinearLayout) findViewById(R.id.reportTeam_progressPanel);
        progressPanel.setVisibility(View.GONE);

        buildResultFinishHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String resultMessage = msg.getData().getString(KEY_REPORT_TEAM_RESULT_MESSAGE);
                onFinishBuildResultThread();
                currentState.setResultMessage(resultMessage);
                showTeamResult();
            }
        };

        setTitle(getResources().getString(R.string.report_team_title));

        refreshTeams(!CLEAR_SELECTED_TEAM);
    }

    private void initListAdapters() {
        DataProvider dataProvider = new DataProvider();

        List<TeamListRecord> items = dataProvider.getTeams(SortColumn.NUMBER);
        adapterByNumber = new TeamsAdapter(this, R.layout.report_team_result_row, items);
        ((TeamFilter) adapterByNumber.getFilter()).initialize(items, currentState);

        items = dataProvider.getTeams(SortColumn.MEMBER);
        adapterByMember = new TeamsAdapter(this, R.layout.report_team_result_row, items);
        ((TeamFilter) adapterByMember.getFilter()).initialize(items, currentState);
    }

    public void refreshTeams() {
        refreshTeams(CLEAR_SELECTED_TEAM);
    }

    private void refreshTeams(boolean clearSelectedTeam) {
        if (clearSelectedTeam) clearSelectedTeam();

        TeamsAdapter adapter = getAdapterBySortColumn(currentState.getSortColumn());
        lvTeams.setAdapter(adapter);
        adapter.getFilter().filter("");
    }

    private void clearSelectedTeam() {
        currentState.setCurrentTeam(null);
    }

    public TeamResultActivityState getCurrentState() {
        return currentState;
    }

    public TeamsAdapter getAdapterBySortColumn(SortColumn sortColumn) {
        if (sortColumn == SortColumn.MEMBER)
            return adapterByMember;
        else
            return adapterByNumber;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
    }

    public TeamsAdapter getCurrentAdapter() {
        return getAdapterBySortColumn(currentState.getSortColumn());
    }

    @Override
    public void onFilterStateChange() {
        refreshTeams();
    }

    private class LvTeamsItemLongClickListener implements OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long itemId) {
            TeamListRecord teamListRecord = ((TeamsAdapter) parent.getAdapter()).getItem(position);
            currentState.setCurrentTeam(teamListRecord.getTeam());

            // hide soft keyboard
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(lvTeams.getApplicationWindowToken(), 0);

            startBuildResultThread();
            return true;
        }
    }

    private void startBuildResultThread() {
        onStartBuildResultThread();
        BuildTeamResultThread thread =
                new BuildTeamResultThread(this, buildResultFinishHandler, currentState.getCurrentTeam());
        thread.start();
    }

    private void onStartBuildResultThread() {
        progressPanel.setVisibility(View.VISIBLE);
        sortButtons.setEnabled(false);
        filterPanel.setEnabled(false);
        lvTeams.setEnabled(false);
    }

    private void onFinishBuildResultThread() {
        progressPanel.setVisibility(View.GONE);
        sortButtons.setEnabled(true);
        filterPanel.setEnabled(true);
        lvTeams.setEnabled(true);
    }

    private void showTeamResult() {
        DialogFragment dialog = new TeamResultDialogFragment();
        dialog.show(getSupportFragmentManager(), "TeamResultDialogFragment");
    }
}
