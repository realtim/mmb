package ru.mmb.datacollector.activity.report.team.search;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_MEMBER;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_NUMBER;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_NUMBER_EXACT;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_STATE;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_TEAM;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_RESULT_MESSAGE;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_SORT_COLUMN;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_SORT_ORDER;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import android.content.SharedPreferences;
import android.os.Bundle;

public class SearchTeamActivityState extends ActivityStateWithTeamAndScanPoint
{
	private SortColumn sortColumn = SortColumn.NUMBER;
	private SortOrder sortOrder = SortOrder.ASC;

	private FilterState filterState = FilterState.SHOW_JUST_NUMBER;
	private boolean filterNumberExact = true;

	private String numberFilter = null;
	private String teamFilter = null;
	private String memberFilter = null;

	private String resultMessage = null;

	public SearchTeamActivityState()
	{
		super("report.team");
	}

	public SortColumn getSortColumn()
	{
		return sortColumn;
	}

	public void setSortColumn(SortColumn sortColumn)
	{
		this.sortColumn = sortColumn;
	}

	public SortOrder getSortOrder()
	{
		return sortOrder;
	}

	public void setSortOrder(SortOrder sortOrder)
	{
		this.sortOrder = sortOrder;
	}

	public void switchSortOrder()
	{
		sortOrder = (sortOrder == SortOrder.ASC) ? SortOrder.DESC : SortOrder.ASC;
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		savedInstanceState.putSerializable(KEY_REPORT_TEAM_SORT_COLUMN, sortColumn);
		savedInstanceState.putSerializable(KEY_REPORT_TEAM_SORT_ORDER, sortOrder);
		savedInstanceState.putSerializable(KEY_REPORT_TEAM_FILTER_STATE, filterState);
		savedInstanceState.putBoolean(KEY_REPORT_TEAM_FILTER_NUMBER_EXACT, filterNumberExact);
		if (numberFilter != null)
		    savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_NUMBER, numberFilter);
		if (teamFilter != null)
		    savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_TEAM, teamFilter);
		if (memberFilter != null)
		    savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_MEMBER, memberFilter);
		if (resultMessage != null)
		    savedInstanceState.putString(KEY_REPORT_TEAM_RESULT_MESSAGE, resultMessage);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		super.load(savedInstanceState);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_SORT_COLUMN))
		    sortColumn =
		        (SortColumn) savedInstanceState.getSerializable(KEY_REPORT_TEAM_SORT_COLUMN);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_SORT_ORDER))
		    sortOrder = (SortOrder) savedInstanceState.getSerializable(KEY_REPORT_TEAM_SORT_ORDER);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_FILTER_STATE))
		    filterState =
		        (FilterState) savedInstanceState.getSerializable(KEY_REPORT_TEAM_FILTER_STATE);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_FILTER_NUMBER_EXACT))
		    filterNumberExact = savedInstanceState.getBoolean(KEY_REPORT_TEAM_FILTER_NUMBER_EXACT);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_FILTER_NUMBER))
		    numberFilter = savedInstanceState.getString(KEY_REPORT_TEAM_FILTER_NUMBER);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_FILTER_TEAM))
		    teamFilter = savedInstanceState.getString(KEY_REPORT_TEAM_FILTER_TEAM);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_FILTER_MEMBER))
		    memberFilter = savedInstanceState.getString(KEY_REPORT_TEAM_FILTER_MEMBER);
		if (savedInstanceState.containsKey(KEY_REPORT_TEAM_RESULT_MESSAGE))
		    resultMessage = savedInstanceState.getString(KEY_REPORT_TEAM_RESULT_MESSAGE);
	}

	public FilterState getFilterState()
	{
		return filterState;
	}

	public void setFilterState(FilterState filterState)
	{
		this.filterState = filterState;
	}

	public boolean isFilterNumberExact()
	{
		return filterNumberExact;
	}

	public void setFilterNumberExact(boolean filterNumberExact)
	{
		this.filterNumberExact = filterNumberExact;
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_COLUMN, getSortColumn().name());
		editor.putString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_ORDER, getSortOrder().name());
		editor.putString(getPrefix() + "." + KEY_REPORT_TEAM_FILTER_STATE, getFilterState().name());
		editor.putBoolean(getPrefix() + "." + KEY_REPORT_TEAM_FILTER_NUMBER_EXACT, isFilterNumberExact());
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		super.loadFromSharedPreferences(preferences);

		String sortColumnName =
		    preferences.getString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_COLUMN, "NUMBER");
		setSortColumn(SortColumn.getByName(sortColumnName));
		String sortOrderName =
		    preferences.getString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_ORDER, "ASC");
		setSortOrder(SortOrder.getByName(sortOrderName));
		String filterStateName =
		    preferences.getString(getPrefix() + "." + KEY_REPORT_TEAM_FILTER_STATE, "SHOW_JUST_NUMBER");
		setFilterState(FilterState.getByName(filterStateName));
		setFilterNumberExact(preferences.getBoolean(getPrefix() + "."
		        + KEY_REPORT_TEAM_FILTER_NUMBER_EXACT, true));
	}

	public String getNumberFilter()
	{
		return numberFilter;
	}

	public int getNumberFilterAsInt()
	{
		return Integer.parseInt(numberFilter);
	}

	public void setNumberFilter(String numberFilter)
	{
		if (isEmptyString(numberFilter))
			this.numberFilter = null;
		else
			this.numberFilter = numberFilter;
	}

	private boolean isEmptyString(String numberFilter)
	{
		return numberFilter != null && numberFilter.trim().length() == 0;
	}

	public String getTeamFilter()
	{
		return teamFilter;
	}

	public void setTeamFilter(String teamFilter)
	{
		if (isEmptyString(teamFilter))
			this.teamFilter = null;
		else
			this.teamFilter = teamFilter;
	}

	public String getMemberFilter()
	{
		return memberFilter;
	}

	public void setMemberFilter(String memberFilter)
	{
		if (isEmptyString(memberFilter))
			this.memberFilter = null;
		else
			this.memberFilter = memberFilter;
	}

	public String getFilterStatusText(SearchTeamActivity context)
	{
		String result = "";
		if (numberFilter != null && numberFilter.length() > 0)
		{
			result +=
			    context.getResources().getString(R.string.report_team_filter_status_number) + " "
			            + numberFilter;
			if (isFilterNumberExact())
			    result +=
			        " "
			                + context.getResources().getString(R.string.report_team_filter_status_exact);
		}
		if (teamFilter != null && teamFilter.length() > 0)
		{
			if (!"".equals(result)) result += "\n";
			result +=
			    context.getResources().getString(R.string.report_team_filter_status_team) + " "
			            + teamFilter;
		}
		if (memberFilter != null && memberFilter.length() > 0)
		{
			if (!"".equals(result)) result += "\n";
			result +=
			    context.getResources().getString(R.string.report_team_filter_status_member) + " "
			            + memberFilter;
		}
		if ("".equals(result))
		    result = context.getResources().getString(R.string.report_team_filter_status_empty);
		return result;
	}

	public String getResultMessage()
	{
		return resultMessage;
	}

	public void setResultMessage(String resultMessage)
	{
		this.resultMessage = resultMessage;
	}
}
