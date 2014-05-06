package ru.mmb.terminal.activity.report.team.search;


public abstract class ModeSwitchable
{
	private final SearchTeamActivityState currentState;

	public ModeSwitchable(SearchTeamActivityState currentState)
	{
		this.currentState = currentState;
	}

	protected abstract void switchToFastMode();

	protected abstract void switchToUsualMode();

	public void switchMode()
	{
		if (currentState.isTeamFastSelect())
			switchToFastMode();
		else
			switchToUsualMode();
	}
}
