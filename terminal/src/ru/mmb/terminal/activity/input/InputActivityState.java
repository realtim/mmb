package ru.mmb.terminal.activity.input;

import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_DISTANCE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_INPUT_MODE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_LEVEL;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_TEAM;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.Constants;
import ru.mmb.terminal.activity.CurrentState;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class InputActivityState extends CurrentState
{
	private Distance currentDistance = null;
	private Level currentLevel = null;
	private InputMode currentInputMode = null;
	private Team currentTeam = null;

	public InputActivityState(String prefix)
	{
		super(prefix);
	}

	public Distance getCurrentDistance()
	{
		return currentDistance;
	}

	public void setCurrentDistance(Distance currentDistance)
	{
		this.currentDistance = currentDistance;
		fireStateChanged();
	}

	public Level getCurrentLevel()
	{
		return currentLevel;
	}

	public void setCurrentLevel(Level currentLevel)
	{
		this.currentLevel = currentLevel;
		fireStateChanged();
	}

	public InputMode getCurrentInputMode()
	{
		return currentInputMode;
	}

	public void setCurrentInputMode(InputMode currentInputMode)
	{
		this.currentInputMode = currentInputMode;
		fireStateChanged();
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		if (currentDistance != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_DISTANCE, currentDistance);
		if (currentLevel != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_LEVEL, currentLevel);
		if (currentInputMode != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_INPUT_MODE, currentInputMode);
		if (currentTeam != null) savedInstanceState.putSerializable(KEY_CURRENT_TEAM, currentTeam);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		if (savedInstanceState == null) return;

		if (savedInstanceState.containsKey(KEY_CURRENT_DISTANCE))
		    currentDistance = (Distance) savedInstanceState.getSerializable(KEY_CURRENT_DISTANCE);
		if (savedInstanceState.containsKey(KEY_CURRENT_LEVEL))
		    currentLevel = (Level) savedInstanceState.getSerializable(KEY_CURRENT_LEVEL);
		if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_MODE))
		    currentInputMode =
		        (InputMode) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_MODE);
		if (savedInstanceState.containsKey(KEY_CURRENT_TEAM))
		    currentTeam = (Team) savedInstanceState.getSerializable(KEY_CURRENT_TEAM);
	}

	@Override
	protected void update()
	{
		DistancesRegistry distances = DistancesRegistry.getInstance();

		if (currentDistance != null)
		{
			Distance updatedDistance = distances.getDistanceById(currentDistance.getDistanceId());
			if (updatedDistance == null)
			{
				currentDistance = null;
				currentLevel = null;
				currentInputMode = null;
			}
			else
			{
				currentDistance = updatedDistance;
			}
		}

		if (currentDistance != null && currentLevel != null)
		{
			Level updatedLevel = currentDistance.getLevelById(currentLevel.getLevelId());
			if (updatedLevel == null)
			{
				currentLevel = null;
				currentInputMode = null;
			}
			else
			{
				currentLevel = updatedLevel;
			}
		}

		if (currentTeam != null)
		{
			TeamsRegistry teams = TeamsRegistry.getInstance();
			Team updatedTeam = teams.getTeamById(currentTeam.getTeamId());
			currentTeam = updatedTeam;
		}
	}

	public boolean isLevelSelected()
	{
		return currentDistance != null && currentLevel != null && currentInputMode != null;
	}

	private String getSelectedLevelString(Activity activity)
	{
		if (!isLevelSelected())
			return activity.getResources().getString(R.string.input_global_no_selected_level);
		else
			return "\"" + currentDistance.getDistanceName() + "\" -> \""
			        + currentLevel.getLevelName() + "\" -> ["
			        + activity.getResources().getString(currentInputMode.getDisplayNameId()) + "]";
	}

	public String getTitleText(Activity activity)
	{
		return getSelectedLevelString(activity);
	}

	@Override
	protected void loadFromExtrasBundle(Bundle extras)
	{
		if (extras.containsKey(KEY_CURRENT_DISTANCE))
		    setCurrentDistance((Distance) extras.getSerializable(KEY_CURRENT_DISTANCE));
		if (extras.containsKey(KEY_CURRENT_LEVEL))
		    setCurrentLevel((Level) extras.getSerializable(KEY_CURRENT_LEVEL));
		if (extras.containsKey(KEY_CURRENT_INPUT_MODE))
		    setCurrentInputMode((InputMode) extras.getSerializable(KEY_CURRENT_INPUT_MODE));
		if (extras.containsKey(KEY_CURRENT_TEAM))
		    setCurrentTeam((Team) extras.getSerializable(KEY_CURRENT_TEAM));
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		SharedPreferences.Editor editor = preferences.edit();
		if (getCurrentDistance() != null)
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_DISTANCE, getCurrentDistance().getDistanceId());
		if (getCurrentLevel() != null)
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_LEVEL, getCurrentLevel().getLevelId());
		if (getCurrentInputMode() != null)
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_INPUT_MODE, getCurrentInputMode().getId());
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		DistancesRegistry distances = DistancesRegistry.getInstance();
		int distanceId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_DISTANCE, -1);
		currentDistance = distances.getDistanceById(distanceId);
		if (currentDistance != null)
		{
			int levelId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_LEVEL, -1);
			currentLevel = currentDistance.getLevelById(levelId);
			if (currentLevel != null)
			{
				int inputModeId =
				    preferences.getInt(getPrefix() + "." + KEY_CURRENT_INPUT_MODE, -1);
				if (inputModeId == -1)
					currentInputMode = null;
				else
					currentInputMode = InputMode.getById(inputModeId);
			}
			else
				currentInputMode = null;
		}
		else
		{
			currentLevel = null;
			currentInputMode = null;
		}
	}

	@Override
	public void prepareStartActivityIntent(Intent intent, int activityRequestId)
	{
		switch (activityRequestId)
		{
			case Constants.REQUEST_CODE_DEFAULT_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_LEVEL_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_TEAM_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY:
			case Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY:
				if (getCurrentDistance() != null)
				    intent.putExtra(KEY_CURRENT_DISTANCE, getCurrentDistance());
				if (getCurrentLevel() != null)
				    intent.putExtra(KEY_CURRENT_LEVEL, getCurrentLevel());
				if (getCurrentInputMode() != null)
				    intent.putExtra(KEY_CURRENT_INPUT_MODE, getCurrentInputMode());
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

	public String getCurrentTeamText(Activity context)
	{
		if (currentTeam == null)
			return context.getResources().getString(R.string.input_team_no_team);
		else
			return currentTeam.getTeamNum() + " " + currentTeam.getTeamName();
	}

	public boolean isTeamSelected()
	{
		return currentTeam != null;
	}

	@Override
	public String toString()
	{
		return "InputActivityState [currentDistance=" + currentDistance + ", currentLevel="
		        + currentLevel + ", currentInputMode=" + currentInputMode + ", currentTeam="
		        + currentTeam + ", toString()=" + super.toString() + "]";
	}
}
