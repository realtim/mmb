package ru.mmb.terminal.activity.input.team;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY;

import java.util.List;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.data.InputDataActivity;
import ru.mmb.terminal.activity.input.team.model.DataProvider;
import ru.mmb.terminal.activity.input.team.model.TeamListRecord;
import ru.mmb.terminal.model.registry.Settings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SearchTeamActivity extends Activity
{
	private static final boolean CLEAR_SELECTED_TEAM = true;

	private SearchTeamActivityState currentState;

	private TeamsAdapter adapterByNumber;
	private TeamsAdapter adapterByMember;

	private ListView lvTeams;
	private TextView labSelectedTeam;
	private Button btnInputData;
	private Button btnMode;

	private FilterPanel filterPanel;
	private SortButtons sortButtons;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new SearchTeamActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_team);

		filterPanel = new FilterPanel(this, currentState);
		sortButtons = new SortButtons(this, currentState);

		lvTeams = (ListView) findViewById(R.id.inputTeam_teamsList);
		initListAdapters();
		lvTeams.setOnItemClickListener(new LvTeamsItemClickListener());

		labSelectedTeam = (TextView) findViewById(R.id.inputTeam_selectedTeamTextView);
		btnInputData = (Button) findViewById(R.id.inputTeam_inputDataButton);
		btnMode = (Button) findViewById(R.id.inputTeam_modeButton);

		btnInputData.setOnClickListener(new InputDataClickListener());
		btnMode.setOnClickListener(new ModeClickListener());

		setTitle(currentState.getTitleText(this));

		refreshTeams(!CLEAR_SELECTED_TEAM);
		refreshSelectedTeamLabel();
		refreshModeButtonState();
		refreshInputButtonsState();
	}

	private void initListAdapters()
	{
		int distanceId = currentState.getCurrentDistance().getDistanceId();
		DataProvider dataProvider = new DataProvider();

		List<TeamListRecord> items = dataProvider.getTeams(distanceId, SortColumn.NUMBER);
		adapterByNumber = new TeamsAdapter(this, R.layout.input_team_row, items);
		((TeamFilter) adapterByNumber.getFilter()).initialize(items, currentState);

		items = dataProvider.getTeams(distanceId, SortColumn.MEMBER);
		adapterByMember = new TeamsAdapter(this, R.layout.input_team_row, items);
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
		refreshSelectedTeamLabel();
		refreshInputButtonsState();
	}

	public TeamsAdapter getAdapterBySortColumn(SortColumn sortColumn)
	{
		if (sortColumn == SortColumn.MEMBER)
			return adapterByMember;
		else
			return adapterByNumber;
	}

	public void refreshSelectedTeamLabel()
	{
		labSelectedTeam.setText(currentState.getCurrentTeamText(this));
	}

	public void refreshInputButtonsState()
	{
		btnInputData.setEnabled(currentState.isTeamSelected());
	}

	private void refreshModeButtonState()
	{
		if (Settings.getInstance().isTeamFastSelect())
			btnMode.setText(getResources().getString(R.string.input_team_mode_usual));
		else
			btnMode.setText(getResources().getString(R.string.input_team_mode_fast));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_INPUT_DATA_ACTIVITY:
				onInputActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onInputActivityResult(int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			if (Settings.getInstance().isTeamClearFilterAfterOk())
			{
				filterPanel.reset();
			}
			if (Settings.getInstance().isTeamFastSelect())
			{
				filterPanel.focusNumberInputAndShowKeyboard();
			}
		}
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

	public void selectTeamAndStartInput()
	{
		if (!Settings.getInstance().isTeamFastSelect()) return;

		// teams are already filtered
		if (lvTeams.getAdapter().isEmpty()) return;

		int teamId = (int) lvTeams.getAdapter().getItemId(0);
		currentState.setCurrentTeam(teamId);

		// hide soft keyboard
		InputMethodManager imm =
		    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(lvTeams.getApplicationWindowToken(), 0);

		startInputDataActivity();
	}

	private void startInputDataActivity()
	{
		Intent intent = new Intent(getApplicationContext(), InputDataActivity.class);
		currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
		startActivityForResult(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
	}

	private class InputDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			startInputDataActivity();
		}
	}

	private class LvTeamsItemClickListener implements OnItemClickListener
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long itemId)
		{
			currentState.setCurrentTeam((int) itemId);
			refreshSelectedTeamLabel();
			refreshInputButtonsState();
		}
	}

	private class ModeClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			boolean newTeamFastSelect = !Settings.getInstance().isTeamFastSelect();
			Settings.getInstance().setTeamFastSelect(Boolean.toString(newTeamFastSelect));
			filterPanel.switchMode();
			sortButtons.switchMode();
			refreshModeButtonState();
		}
	}
}
