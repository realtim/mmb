package ru.mmb.terminal.activity.report.team.search;

import static ru.mmb.terminal.activity.Constants.KEY_REPORT_TEAM_RESULT_MESSAGE;

import java.util.List;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.report.team.search.model.DataProvider;
import ru.mmb.terminal.activity.report.team.search.model.TeamListRecord;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

public class SearchTeamActivity extends FragmentActivity
{
	private static final boolean CLEAR_SELECTED_TEAM = true;

	private SearchTeamActivityState currentState;

	private TeamsAdapter adapterByNumber;
	private TeamsAdapter adapterByMember;

	private ListView lvTeams;
	private Button btnMode;
	private LinearLayout progressPanel;

	private FilterPanel filterPanel;
	private SortButtons sortButtons;

	private Handler buildResultFinishHandler;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new SearchTeamActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.report_team_search);

		filterPanel = new FilterPanel(this, currentState);
		sortButtons = new SortButtons(this, currentState);

		lvTeams = (ListView) findViewById(R.id.reportTeam_teamsList);
		initListAdapters();
		lvTeams.setOnItemLongClickListener(new LvTeamsItemLongClickListener());

		btnMode = (Button) findViewById(R.id.reportTeam_modeButton);
		btnMode.setOnClickListener(new ModeClickListener());

		progressPanel = (LinearLayout) findViewById(R.id.reportTeam_progressPanel);
		progressPanel.setVisibility(View.GONE);

		buildResultFinishHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				String resultMessage = msg.getData().getString(KEY_REPORT_TEAM_RESULT_MESSAGE);
				onFinishBuildResultThread();
				currentState.setResultMessage(resultMessage);
				showTeamResult();
			}
		};

		setTitle(getResources().getString(R.string.report_team_title));

		refreshTeams(!CLEAR_SELECTED_TEAM);
		refreshModeButtonState();
	}

	private void initListAdapters()
	{
		DataProvider dataProvider = new DataProvider();

		List<TeamListRecord> items = dataProvider.getTeams(SortColumn.NUMBER);
		adapterByNumber = new TeamsAdapter(this, R.layout.report_team_search_row, items);
		((TeamFilter) adapterByNumber.getFilter()).initialize(items, currentState);

		items = dataProvider.getTeams(SortColumn.MEMBER);
		adapterByMember = new TeamsAdapter(this, R.layout.report_team_search_row, items);
		((TeamFilter) adapterByMember.getFilter()).initialize(items, currentState);

		filterPanel.addObserversToAdapters();
	}

	public void refreshTeams()
	{
		refreshTeams(CLEAR_SELECTED_TEAM);
	}

	private void refreshTeams(boolean clearSelectedTeam)
	{
		if (clearSelectedTeam) clearSelectedTeam();

		TeamsAdapter adapter = getAdapterBySortColumn(currentState.getSortColumn());
		lvTeams.setAdapter(adapter);
		adapter.getFilter().filter("");
	}

	private void clearSelectedTeam()
	{
		currentState.setCurrentTeam(null);
	}

	public SearchTeamActivityState getCurrentState()
	{
		return currentState;
	}

	public TeamsAdapter getAdapterBySortColumn(SortColumn sortColumn)
	{
		if (sortColumn == SortColumn.MEMBER)
			return adapterByMember;
		else
			return adapterByNumber;
	}

	private void refreshModeButtonState()
	{
		if (currentState.isTeamFastSelect())
			btnMode.setText(getResources().getString(R.string.report_team_mode_usual));
		else
			btnMode.setText(getResources().getString(R.string.report_team_mode_fast));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
	}

	public TeamsAdapter getCurrentAdapter()
	{
		return getAdapterBySortColumn(currentState.getSortColumn());
	}

	public void selectTeamAndShowReport()
	{
		if (!currentState.isTeamFastSelect()) return;

		// teams are already filtered
		if (lvTeams.getAdapter().isEmpty()) return;

		TeamListRecord teamListRecord = (TeamListRecord) lvTeams.getAdapter().getItem(0);
		currentState.setCurrentTeam(teamListRecord.getTeam());

		// hide soft keyboard
		InputMethodManager imm =
		    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(lvTeams.getApplicationWindowToken(), 0);

		startBuildResultThread();
	}

	private class LvTeamsItemLongClickListener implements OnItemLongClickListener
	{
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long itemId)
		{
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

	private void startBuildResultThread()
	{
		onStartBuildResultThread();
		BuildTeamResultThread thread =
		    new BuildTeamResultThread(this, buildResultFinishHandler, currentState.getCurrentTeam());
		thread.start();
	}

	private void onStartBuildResultThread()
	{
		progressPanel.setVisibility(View.VISIBLE);
		sortButtons.setEnabled(false);
		filterPanel.setEnabled(false);
		btnMode.setEnabled(false);
		lvTeams.setEnabled(false);
	}

	private void onFinishBuildResultThread()
	{
		progressPanel.setVisibility(View.GONE);
		sortButtons.setEnabled(true);
		filterPanel.setEnabled(true);
		btnMode.setEnabled(true);
		lvTeams.setEnabled(true);
	}

	private void showTeamResult()
	{
		DialogFragment dialog = new TeamResultDialogFragment();
		dialog.show(getSupportFragmentManager(), "TeamResultDialogFragment");
	}

	private class ModeClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			boolean newTeamFastSelect = !currentState.isTeamFastSelect();
			currentState.setTeamFastSelect(newTeamFastSelect);
			filterPanel.switchMode();
			sortButtons.switchMode();
			refreshModeButtonState();
		}
	}
}
