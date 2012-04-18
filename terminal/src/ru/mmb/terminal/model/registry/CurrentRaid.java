package ru.mmb.terminal.model.registry;

import ru.mmb.terminal.db.TerminalDB;

public class CurrentRaid
{
	private static Integer id = null;

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
	}
}
