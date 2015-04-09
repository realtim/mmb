package ru.mmb.datacollector.activity.report.global;

import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_GLOBAL_REPORT_MODE;
import static ru.mmb.datacollector.activity.Constants.KEY_REPORT_GLOBAL_SELECTED_TEAMS;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ReportGlobalResultActivityState extends ActivityStateWithTeamAndScanPoint
{
	private GlobalReportMode reportMode = GlobalReportMode.ALL_TEAMS;
	private String selectedTeams = "";

	public ReportGlobalResultActivityState()
	{
		super("report.global");
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		savedInstanceState.putSerializable(KEY_REPORT_GLOBAL_REPORT_MODE, reportMode);
		savedInstanceState.putString(KEY_REPORT_GLOBAL_SELECTED_TEAMS, selectedTeams);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		super.load(savedInstanceState);
		if (savedInstanceState.containsKey(KEY_REPORT_GLOBAL_REPORT_MODE))
		    reportMode =
		        (GlobalReportMode) savedInstanceState.getSerializable(KEY_REPORT_GLOBAL_REPORT_MODE);
		if (savedInstanceState.containsKey(KEY_REPORT_GLOBAL_SELECTED_TEAMS))
		    selectedTeams = savedInstanceState.getString(KEY_REPORT_GLOBAL_SELECTED_TEAMS);
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(getPrefix() + "." + KEY_REPORT_GLOBAL_REPORT_MODE, reportMode.name());
		editor.putString(getPrefix() + "." + KEY_REPORT_GLOBAL_SELECTED_TEAMS, selectedTeams);
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		super.loadFromSharedPreferences(preferences);
		String reportModeName =
		    preferences.getString(getPrefix() + "." + KEY_REPORT_GLOBAL_REPORT_MODE, "ALL_TEAMS");
		reportMode = GlobalReportMode.getByName(reportModeName);
		selectedTeams =
		    preferences.getString(getPrefix() + "." + KEY_REPORT_GLOBAL_SELECTED_TEAMS, "");
	}

	public String getSelectedTeams()
	{
		return selectedTeams;
	}

	public void setSelectedTeams(String selectedTeams)
	{
		this.selectedTeams = selectedTeams;
	}

	public GlobalReportMode getReportMode()
	{
		return reportMode;
	}

	public void setReportMode(GlobalReportMode reportMode)
	{
		this.reportMode = reportMode;
	}
}
