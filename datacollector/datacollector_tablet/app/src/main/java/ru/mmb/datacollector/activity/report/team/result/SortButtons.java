package ru.mmb.datacollector.activity.report.team.result;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

import ru.mmb.datacollector.R;

public class SortButtons
{
	private final Button btnSortByNumber;
	private final Button btnSortByTeam;
	private final Button btnSortByMember;

	private final TeamResultActivityState currentState;
	private final TeamResultActivity teamResultActivity;

	private final Map<SortColumn, String> buttonNames = new HashMap<SortColumn, String>();
	private final Map<SortColumn, Button> buttons = new HashMap<SortColumn, Button>();

	private SortColumn prevSortColumn = SortColumn.NUMBER;

	public SortButtons(TeamResultActivity context, TeamResultActivityState currentState)
	{
		this.btnSortByNumber = (Button) context.findViewById(R.id.reportTeam_sortByNumberButton);
		this.btnSortByTeam = (Button) context.findViewById(R.id.reportTeam_sortByTeamButton);
		this.btnSortByMember = (Button) context.findViewById(R.id.reportTeam_sortByMemberButton);

		this.currentState = currentState;

		this.teamResultActivity = context;

		initButtonNames(context);
		initButtons();
		setOnClickListener(new SortButtonClickListener());
		refreshButtonNames();

		prevSortColumn = currentState.getSortColumn();
	}

	private void initButtonNames(Context context)
	{
		buttonNames.put(SortColumn.NUMBER, context.getResources().getString(R.string.report_team_sort_num));
		buttonNames.put(SortColumn.TEAM, context.getResources().getString(R.string.report_team_sort_team));
		buttonNames.put(SortColumn.MEMBER, context.getResources().getString(R.string.report_team_sort_member));
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
			teamResultActivity.refreshTeams();
		}
	}

	public void setEnabled(boolean value)
	{
		btnSortByNumber.setEnabled(value);
		btnSortByTeam.setEnabled(value);
		btnSortByMember.setEnabled(value);
	}
}
