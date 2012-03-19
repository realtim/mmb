package ru.mmb.terminal.activity.input.team;

import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY;
import static ru.mmb.terminal.activity.Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY;

import java.util.List;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.input.data.InputDataActivity;
import ru.mmb.terminal.activity.input.team.model.DataProvider;
import ru.mmb.terminal.activity.input.team.model.TeamListRecord;
import ru.mmb.terminal.activity.input.withdraw.WithdrawMemberActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
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
	private Button btnWithdraw;
	private LinearLayout panelProceedInput;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentState = new SearchTeamActivityState();
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_team);

		new FilterPanel(this, currentState);
		new SortButtons(this, currentState);

		lvTeams = (ListView) findViewById(R.id.inputTeam_teamsList);
		initListAdapters();
		lvTeams.setOnItemClickListener(new LvTeamsItemClickListener());

		panelProceedInput = (LinearLayout) findViewById(R.id.inputTeam_proceedInputPanel);
		labSelectedTeam = (TextView) findViewById(R.id.inputTeam_selectedTeamTextView);
		btnInputData = (Button) findViewById(R.id.inputTeam_inputDataButton);
		btnWithdraw = (Button) findViewById(R.id.inputTeam_withdrawMemberButton);

		btnInputData.setOnClickListener(new InputDataClickListener());
		btnWithdraw.setOnClickListener(new WithdrawMemberClickListener());

		initProceedPanelVisibility();

		setTitle(currentState.getTitleText(this));

		refreshTeams(!CLEAR_SELECTED_TEAM);
		refreshSelectedTeamLabel();
		refreshInputButtonsState();
	}

	private void initListAdapters()
	{
		int distanceId = currentState.getCurrentDistance().getId();
		DataProvider dataProvider = new DataProvider();

		List<TeamListRecord> items = dataProvider.getTeams(distanceId, SortColumn.NUMBER);
		adapterByNumber = new TeamsAdapter(this, R.layout.input_team_row, items);
		((TeamFilter) adapterByNumber.getFilter()).initialize(items, currentState);

		items = dataProvider.getTeams(distanceId, SortColumn.MEMBER);
		adapterByMember = new TeamsAdapter(this, R.layout.input_team_row, items);
		((TeamFilter) adapterByMember.getFilter()).initialize(items, currentState);
	}

	private void initProceedPanelVisibility()
	{
		if (currentState.getActivityMode() == ActivityMode.SEARCH_TEAM)
		{
			panelProceedInput.getLayoutParams().height = 0;
			((LayoutParams) panelProceedInput.getLayoutParams()).topMargin = 0;
			((LayoutParams) panelProceedInput.getLayoutParams()).bottomMargin = 0;
		}
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

	private TeamsAdapter getAdapterBySortColumn(SortColumn sortColumn)
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
		btnWithdraw.setEnabled(currentState.isTeamSelected());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_INPUT_DATA_ACTIVITY:
				onInputDataActivityResult(resultCode, data);
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void onInputDataActivityResult(int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			setResult(RESULT_OK, new Intent());
			finish();
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

	private class InputDataClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), InputDataActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_INPUT_DATA_ACTIVITY);
		}
	}

	private class WithdrawMemberClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), WithdrawMemberActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY);
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
}
