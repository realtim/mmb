package ru.mmb.terminal.activity.input.team;

import ru.mmb.terminal.model.registry.Settings;

public abstract class ModeSwitchable
{
	protected abstract void switchToFastMode();

	protected abstract void switchToUsualMode();

	public void switchMode()
	{
		if (Settings.getInstance().isTeamFastSelect())
			switchToFastMode();
		else
			switchToUsualMode();
	}
}
