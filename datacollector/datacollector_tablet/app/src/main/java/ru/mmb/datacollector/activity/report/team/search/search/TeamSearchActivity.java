package ru.mmb.datacollector.activity.report.team.search.search;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.report.team.FilterPanel;
import ru.mmb.datacollector.activity.report.team.FilterStateChangeListener;

public class TeamSearchActivity extends Activity implements FilterStateChangeListener {
    private TeamSearchActivityState currentState;

    private FilterPanel filterPanel;

    private ListView lvTeams;
    private TeamsAdapter teamsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentState = new TeamSearchActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.report_team_search);

        int[] filterFieldIds = {R.id.teamSearch_filterClearButton, R.id.teamSearch_filterHideButton,
                R.id.teamSearch_filterStatusTextView, R.id.teamSearch_filterNumberEdit, R.id.teamSearch_filterNumberExactCheck,
                R.id.teamSearch_filterTeamEdit, R.id.teamSearch_filterMemberEdit, R.id.teamSearch_filterNumberPanel,
                R.id.teamSearch_filterTeamAndMemberPanel};
        filterPanel = new FilterPanel(this, currentState.getFilterPanelState(), filterFieldIds);
        filterPanel.addFilterStateChangeListener(this);

        lvTeams = (ListView) findViewById(R.id.teamSearch_teamsList);
        initListAdapter();
        lvTeams.setAdapter(teamsAdapter);

        setTitle(getResources().getString(R.string.report_team_search_title));

        refreshTeams();
    }

    private void initListAdapter() {
        DataProvider dataProvider = new DataProvider();
        List<TeamListRecord> items = dataProvider.getTeams(currentState.getCurrentScanPoint());
        teamsAdapter = new TeamsAdapter(this, R.layout.report_team_search_row, items);
        ((TeamFilter) teamsAdapter.getFilter()).initialize(items, currentState);
    }

    private void refreshTeams() {
        teamsAdapter.getFilter().filter("");
    }

    public TeamSearchActivityState getCurrentState() {
        return currentState;
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

    @Override
    public void onFilterStateChange() {
        refreshTeams();
    }
}
