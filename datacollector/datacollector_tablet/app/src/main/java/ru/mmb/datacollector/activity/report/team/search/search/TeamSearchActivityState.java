package ru.mmb.datacollector.activity.report.team.search.search;

import android.content.SharedPreferences;
import android.os.Bundle;

import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.report.team.FilterPanelState;

public class TeamSearchActivityState extends ActivityStateWithTeamAndScanPoint
{
    private FilterPanelState filterPanelState = new FilterPanelState("team.search");

	public TeamSearchActivityState()
	{
		super("team.search");
	}

    public FilterPanelState getFilterPanelState() {
        return filterPanelState;
    }

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		filterPanelState.save(savedInstanceState);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		super.load(savedInstanceState);
		filterPanelState.load(savedInstanceState);
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);
		filterPanelState.saveToSharedPreferences(preferences);
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		super.loadFromSharedPreferences(preferences);
        filterPanelState.loadFromSharedPreferences(preferences);
	}
}
