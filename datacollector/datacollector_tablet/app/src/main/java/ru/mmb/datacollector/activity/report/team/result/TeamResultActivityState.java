package ru.mmb.datacollector.activity.report.team.result;

import android.content.SharedPreferences;
import android.os.Bundle;

import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.report.team.FilterPanelState;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_RESULT_MESSAGE;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_SORT_COLUMN;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_SORT_ORDER;

public class TeamResultActivityState extends ActivityStateWithTeamAndScanPoint
{
	private SortColumn sortColumn = SortColumn.NUMBER;
	private SortOrder sortOrder = SortOrder.ASC;

    private String resultMessage = null;

    private FilterPanelState filterPanelState = new FilterPanelState("report.team");

	public TeamResultActivityState()
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

    public String getResultMessage()
    {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage)
    {
        this.resultMessage = resultMessage;
    }

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		savedInstanceState.putSerializable(KEY_REPORT_TEAM_SORT_COLUMN, sortColumn);
		savedInstanceState.putSerializable(KEY_REPORT_TEAM_SORT_ORDER, sortOrder);
        if (resultMessage != null)
            savedInstanceState.putString(KEY_REPORT_TEAM_RESULT_MESSAGE, resultMessage);
        filterPanelState.save(savedInstanceState);
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
        if (savedInstanceState.containsKey(KEY_REPORT_TEAM_RESULT_MESSAGE))
            resultMessage = savedInstanceState.getString(KEY_REPORT_TEAM_RESULT_MESSAGE);
        filterPanelState.load(savedInstanceState);
	}

	public FilterPanelState getFilterPanelState()
	{
		return filterPanelState;
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_COLUMN, getSortColumn().name());
		editor.putString(getPrefix() + "." + KEY_REPORT_TEAM_SORT_ORDER, getSortOrder().name());
		editor.commit();

        filterPanelState.saveToSharedPreferences(preferences);
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

		filterPanelState.loadFromSharedPreferences(preferences);
	}
}
