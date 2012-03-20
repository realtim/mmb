package ru.mmb.terminal.activity.input;

import static ru.mmb.terminal.activity.Constants.KEY_ACTIVE_USER;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_DISTANCE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_INPUT_MODE;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_LAP;
import static ru.mmb.terminal.activity.Constants.KEY_CURRENT_TEAM;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.Constants;
import ru.mmb.terminal.activity.CurrentState;
import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Lap;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.User;
import ru.mmb.terminal.model.registry.DistancesRegistry;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class InputActivityState extends CurrentState
{
	private User activeUser = null;
	private Distance currentDistance = null;
	private Lap currentLap = null;
	private InputMode currentInputMode = null;
	private Team currentTeam = null;

	public InputActivityState(String prefix)
	{
		super(prefix);
	}

	public User getActiveUser()
	{
		return activeUser;
	}

	public void setActiveUser(User activeUser)
	{
		this.activeUser = activeUser;
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

	public Lap getCurrentLap()
	{
		return currentLap;
	}

	public void setCurrentLap(Lap currentLap)
	{
		this.currentLap = currentLap;
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
		if (activeUser != null) savedInstanceState.putSerializable(KEY_ACTIVE_USER, activeUser);
		if (currentDistance != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_DISTANCE, currentDistance);
		if (currentLap != null) savedInstanceState.putSerializable(KEY_CURRENT_LAP, currentLap);
		if (currentInputMode != null)
		    savedInstanceState.putSerializable(KEY_CURRENT_INPUT_MODE, currentInputMode);
		if (currentTeam != null) savedInstanceState.putSerializable(KEY_CURRENT_TEAM, currentTeam);
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		if (savedInstanceState == null) return;

		if (savedInstanceState.containsKey(KEY_ACTIVE_USER))
		    activeUser = (User) savedInstanceState.getSerializable(KEY_ACTIVE_USER);
		if (savedInstanceState.containsKey(KEY_CURRENT_DISTANCE))
		    currentDistance = (Distance) savedInstanceState.getSerializable(KEY_CURRENT_DISTANCE);
		if (savedInstanceState.containsKey(KEY_CURRENT_LAP))
		    currentLap = (Lap) savedInstanceState.getSerializable(KEY_CURRENT_LAP);
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
			Distance updatedDistance = distances.getDistanceById(currentDistance.getId());
			if (updatedDistance == null)
			{
				currentDistance = null;
				currentLap = null;
				currentInputMode = null;
			}
			else
			{
				currentDistance = updatedDistance;
			}
		}

		if (currentDistance != null && currentLap != null)
		{
			Lap updatedLap = currentDistance.getLapById(currentLap.getId());
			if (updatedLap == null)
			{
				currentLap = null;
				currentInputMode = null;
			}
			else
			{
				currentLap = updatedLap;
			}
		}

		if (currentTeam != null)
		{
			TeamsRegistry teams = TeamsRegistry.getInstance();
			Team updatedTeam = teams.getTeamById(currentTeam.getId());
			currentTeam = updatedTeam;
		}
	}

	public boolean isLapSelected()
	{
		return currentDistance != null && currentLap != null && currentInputMode != null;
	}

	private String getSelectedLapString(Activity activity)
	{
		if (!isLapSelected())
			return activity.getResources().getString(R.string.input_global_no_selected_lap);
		else
			return "\"" + currentDistance.getName() + "\" -> \"" + currentLap.getName() + "\" -> ["
			        + activity.getResources().getString(currentInputMode.getDisplayNameId()) + "]";
	}

	public String getTitleText(Activity activity)
	{
		String result = "";
		if (getActiveUser() != null) result += getActiveUser().getName();
		result += " " + getSelectedLapString(activity);
		return result;
	}

	@Override
	protected void loadFromExtrasBundle(Bundle extras)
	{
		if (extras.containsKey(KEY_ACTIVE_USER))
		    setActiveUser((User) extras.getSerializable(KEY_ACTIVE_USER));
		if (extras.containsKey(KEY_CURRENT_DISTANCE))
		    setCurrentDistance((Distance) extras.getSerializable(KEY_CURRENT_DISTANCE));
		if (extras.containsKey(KEY_CURRENT_LAP))
		    setCurrentLap((Lap) extras.getSerializable(KEY_CURRENT_LAP));
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
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_DISTANCE, getCurrentDistance().getId());
		if (getCurrentLap() != null)
		    editor.putInt(getPrefix() + "." + KEY_CURRENT_LAP, getCurrentLap().getId());
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
			int lapId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_LAP, -1);
			currentLap = currentDistance.getLapById(lapId);
			if (currentLap != null)
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
			currentLap = null;
			currentInputMode = null;
		}
	}

	@Override
	public void prepareStartActivityIntent(Intent intent, int activityRequestId)
	{
		switch (activityRequestId)
		{
			case Constants.REQUEST_CODE_DEFAULT_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_LAP_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_TEAM_ACTIVITY:
			case Constants.REQUEST_CODE_INPUT_DATA_ACTIVITY:
			case Constants.REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY:
				if (getActiveUser() != null) intent.putExtra(KEY_ACTIVE_USER, getActiveUser());
				if (getCurrentDistance() != null)
				    intent.putExtra(KEY_CURRENT_DISTANCE, getCurrentDistance());
				if (getCurrentLap() != null) intent.putExtra(KEY_CURRENT_LAP, getCurrentLap());
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
			return currentTeam.getNumber() + " " + currentTeam.getName();
	}

	public boolean isTeamSelected()
	{
		return currentTeam != null;
	}

	@Override
	public String toString()
	{
		return "InputActivityState [activeUser=" + activeUser + ", currentDistance="
		        + currentDistance + ", currentLap=" + currentLap + ", currentInputMode="
		        + currentInputMode + ", currentTeam=" + currentTeam + "]";
	}
}
