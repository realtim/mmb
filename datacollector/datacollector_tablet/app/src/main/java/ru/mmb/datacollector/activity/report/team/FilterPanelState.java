package ru.mmb.datacollector.activity.report.team;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import ru.mmb.datacollector.R;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_MEMBER;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_NUMBER;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_NUMBER_EXACT;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_STATE;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_TEAM_FILTER_TEAM;

public class FilterPanelState {
    private final String prefix;

    private FilterState filterState = FilterState.SHOW_JUST_NUMBER;
    private boolean filterNumberExact = true;

    private String numberFilter = null;
    private String teamFilter = null;
    private String memberFilter = null;

    public FilterPanelState(String prefix)
    {
        this.prefix = prefix;
    }

    public void save(Bundle savedInstanceState)
    {
        savedInstanceState.putSerializable(KEY_REPORT_TEAM_FILTER_STATE, filterState);
        savedInstanceState.putBoolean(KEY_REPORT_TEAM_FILTER_NUMBER_EXACT, filterNumberExact);
        if (numberFilter != null)
            savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_NUMBER, numberFilter);
        if (teamFilter != null)
            savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_TEAM, teamFilter);
        if (memberFilter != null)
            savedInstanceState.putString(KEY_REPORT_TEAM_FILTER_MEMBER, memberFilter);
    }

    public void load(Bundle savedInstanceState)
    {
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

    public void saveToSharedPreferences(SharedPreferences preferences)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(prefix + "." + KEY_REPORT_TEAM_FILTER_STATE, getFilterState().name());
        editor.putBoolean(prefix + "." + KEY_REPORT_TEAM_FILTER_NUMBER_EXACT, isFilterNumberExact());
        editor.commit();
    }

    public void loadFromSharedPreferences(SharedPreferences preferences)
    {
        String filterStateName =
                preferences.getString(prefix + "." + KEY_REPORT_TEAM_FILTER_STATE, "SHOW_JUST_NUMBER");
        setFilterState(FilterState.getByName(filterStateName));
        setFilterNumberExact(preferences.getBoolean(prefix + "."
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

    public String getFilterStatusText(Activity context)
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
}
