package ru.mmb.datacollector.model.registry;

public class CurrentRaid
{
	public static int getId()
	{
		return Settings.getInstance().getCurrentRaidId();
	}
}
