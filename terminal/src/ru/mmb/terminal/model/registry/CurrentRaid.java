package ru.mmb.terminal.model.registry;


public class CurrentRaid
{
	// TODO restore raid loading from database
	/*private static Integer id = null;

	public static int getId()
	{
		if (id == null)
		{
			id = loadCurrentRaidId();
		}
		return id;
	}

	private static Integer loadCurrentRaidId()
	{
		return new Integer(TerminalDB.getInstance().getCurrentRaidId());
	}*/

	public static int getId()
	{
		return Settings.getInstance().getCurrentRaidId();
	}
}
