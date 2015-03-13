package ru.mmb.datacollector.activity;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_SCAN_POINT;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_TEAM;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ActivityStateWithTeamAndScanPoint extends CurrentState
{
	private ScanPoint currentScanPoint = null;
	private Team currentTeam = null;

	public ActivityStateWithTeamAndScanPoint(String prefix)
	{
		super(prefix);
	}

	public ScanPoint getCurrentScanPoint()
	{
		return currentScanPoint;
	}

	public void setCurrentScanPoint(ScanPoint currentScanPoint)
	{
		this.currentScanPoint = currentScanPoint;
		fireStateChanged();
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		if (currentScanPoint != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_SCAN_POINT, currentScanPoint);
		if (currentTeam != null) savedInstanceState.putSerializable(KEY_CURRENT_TEAM, currentTeam);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		if (savedInstanceState == null) return;

		if (savedInstanceState.containsKey(KEY_CURRENT_SCAN_POINT))
		    currentScanPoint =
		        (ScanPoint) savedInstanceState.getSerializable(KEY_CURRENT_SCAN_POINT);
		if (savedInstanceState.containsKey(KEY_CURRENT_TEAM))
		    currentTeam = (Team) savedInstanceState.getSerializable(KEY_CURRENT_TEAM);
	}

	@Override
	protected void update(boolean fromSavedBundle)
	{
		if (currentScanPoint != null)
		{
			ScanPointsRegistry scanPoints = ScanPointsRegistry.getInstance();
			ScanPoint updatedScanPoint =
			    scanPoints.getScanPointById(currentScanPoint.getScanPointId());
			currentScanPoint = updatedScanPoint;
		}

		if (currentTeam != null)
		{
			TeamsRegistry teams = TeamsRegistry.getInstance();
			Team updatedTeam = teams.getTeamById(currentTeam.getTeamId());
			currentTeam = updatedTeam;
		}
	}

	public boolean isScanPointSelected()
	{
		return currentScanPoint != null;
	}

	private String getSelectedScanPointString(Activity activity)
	{
		if (!isScanPointSelected())
			return activity.getResources().getString(R.string.input_global_no_selected_scan_point);
		else
			return currentScanPoint.getScanPointName();
	}

	public String getScanPointText(Activity activity)
	{
		return getSelectedScanPointString(activity);
	}

	@Override
	protected void loadFromExtrasBundle(Bundle extras)
	{
		if (extras.containsKey(KEY_CURRENT_SCAN_POINT))
		    currentScanPoint = (ScanPoint) extras.getSerializable(KEY_CURRENT_SCAN_POINT);
		if (extras.containsKey(KEY_CURRENT_TEAM))
		    setCurrentTeam((Team) extras.getSerializable(KEY_CURRENT_TEAM));
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		SharedPreferences.Editor editor = preferences.edit();
		if (getCurrentScanPoint() != null)
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_SCAN_POINT, getCurrentScanPoint().getScanPointId());
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		ScanPointsRegistry scanPoints = ScanPointsRegistry.getInstance();
		int scanPointId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_SCAN_POINT, -1);
		currentScanPoint = scanPoints.getScanPointById(scanPointId);
	}

	@Override
	public void prepareStartActivityIntent(Intent intent, int activityRequestId)
	{
		switch (activityRequestId)
		{
			case Constants.REQUEST_CODE_DEFAULT_ACTIVITY:
			case Constants.REQUEST_CODE_SCAN_POINT_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_HISTORY_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY:
			case Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY:
				if (getCurrentScanPoint() != null)
				    intent.putExtra(KEY_CURRENT_SCAN_POINT, getCurrentScanPoint());
				if (getCurrentTeam() != null) intent.putExtra(KEY_CURRENT_TEAM, getCurrentTeam());
		}
	}

	public Team getCurrentTeam()
	{
		return currentTeam;
	}

	public void setCurrentTeam(Team currentTeam)
	{
		this.currentTeam = currentTeam;
	}

	public void setCurrentTeam(int teamId)
	{
		this.currentTeam = TeamsRegistry.getInstance().getTeamById(teamId);
	}

	public boolean isTeamSelected()
	{
		return currentTeam != null;
	}

	public LevelPoint getLevelPointForTeam()
	{
		Team team = getCurrentTeam();
		if (team != null)
		{
			return getCurrentScanPoint().getLevelPointByDistance(team.getDistanceId());
		}
		else
		{
			throw new RuntimeException("ActivityStateWithTeamAndScanPoint.getLevelPointForTeam team not selected.");
		}
	}
}
