package ru.mmb.terminal.activity.input.team;

import java.util.HashMap;
import java.util.Map;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SortButtons extends ModeSwitchable
{
	private final Button btnSortByNumber;
	private final Button btnSortByTeam;
	private final Button btnSortByMember;

	private final SearchTeamActivityState currentState;
	private final SearchTeamActivity searchTeamActivity;

	private final Map<SortColumn, String> buttonNames = new HashMap<SortColumn, String>();
	private final Map<SortColumn, Button> buttons = new HashMap<SortColumn, Button>();

	private SortColumn prevSortColumn = SortColumn.NUMBER;

	public SortButtons(SearchTeamActivity context, SearchTeamActivityState currentState)
	{
		this.btnSortByNumber = (Button) context.findViewById(R.id.inputTeam_sortByNumberButton);
		this.btnSortByTeam = (Button) context.findViewById(R.id.inputTeam_sortByTeamButton);
		this.btnSortByMember = (Button) context.findViewById(R.id.inputTeam_sortByMemberButton);

		this.currentState = currentState;

		this.searchTeamActivity = context;

		initButtonNames(context);
		initButtons();
		setOnClickListener(new SortButtonClickListener());
		refreshButtonNames();

		if (Settings.getInstance().isTeamFastSelect()) switchToFastMode();

		prevSortColumn = currentState.getSortColumn();
	}

	@Override
	protected void switchToFastMode()
	{
		currentState.setSortColumn(SortColumn.NUMBER);
		currentState.setSortOrder(SortOrder.ASC);
		refreshButtonNames();

		btnSortByTeam.setEnabled(false);
		btnSortByMember.setEnabled(false);
	}

	@Override
	protected void switchToUsualMode()
	{
		btnSortByTeam.setEnabled(true);
		btnSortByMember.setEnabled(true);
	}

	private void initButtonNames(Context context)
	{
		buttonNames.put(SortColumn.NUMBER, context.getResources().getString(R.string.input_team_sort_num));
		buttonNames.put(SortColumn.TEAM, context.getResources().getString(R.string.input_team_sort_team));
		buttonNames.put(SortColumn.MEMBER, context.getResources().getString(R.string.input_team_sort_member));
	}

	private void initButtons()
	{
		buttons.put(SortColumn.NUMBER, btnSortByNumber);
		buttons.put(SortColumn.TEAM, btnSortByTeam);
		buttons.put(SortColumn.MEMBER, btnSortByMember);
	}

	private void setOnClickListener(OnClickListener listener)
	{
		btnSortByNumber.setOnClickListener(listener);
		btnSortByTeam.setOnClickListener(listener);
		btnSortByMember.setOnClickListener(listener);
	}

	public void refreshButtonNames()
	{
		refreshButtonName(SortColumn.NUMBER);
		refreshButtonName(SortColumn.TEAM);
		refreshButtonName(SortColumn.MEMBER);
	}

	private void refreshButtonName(SortColumn sortColumn)
	{
		String name = buttonNames.get(sortColumn);
		if (currentState.getSortColumn() == sortColumn)
		{
			if (currentState.getSortOrder() == SortOrder.ASC)
				name += " >";
			else
				name += " <";
		}
		buttons.get(sortColumn).setText(name);
	}

	public class SortButtonClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			SortColumn newSortColumn = SortColumn.NUMBER;
			if (v == btnSortByTeam) newSortColumn = SortColumn.TEAM;
			if (v == btnSortByMember) newSortColumn = SortColumn.MEMBER;

			if (prevSortColumn == newSortColumn)
				currentState.switchSortOrder();
			else
			{
				currentState.setSortColumn(newSortColumn);
				currentState.setSortOrder(SortOrder.ASC);
				prevSortColumn = newSortColumn;
			}
			refreshButtonNames();
			searchTeamActivity.refreshTeams();
		}
	}
}
