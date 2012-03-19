package ru.mmb.terminal.db;

import java.util.Date;
import java.util.List;

import ru.mmb.terminal.activity.input.InputMode;
import ru.mmb.terminal.model.Lap;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.User;
import ru.mmb.terminal.util.ExternalStorage;
import android.database.sqlite.SQLiteDatabase;

public class TerminalDB
{
	private static TerminalDB instance = null;

	private final SQLiteDatabase db;
	private final Withdraw withdraw;
	private final InputData inputData;

	public static TerminalDB getInstance()
	{
		if (instance == null)
		{
			instance = new TerminalDB();
		}
		return instance;
	}

	private TerminalDB()
	{
		db =
		    SQLiteDatabase.openDatabase(ExternalStorage.getDir() + "/mmb/db/terminal.db", null, SQLiteDatabase.OPEN_READWRITE);
		withdraw = new Withdraw(db);
		inputData = new InputData(db);
	}

	public List<Participant> getWithdrawnMembers(Lap lap, Team team)
	{
		return withdraw.getWithdrawnMembers(lap, team);
	}

	public void saveWithdrawnMembers(Lap lap, Team team, User currentUser,
	        List<Participant> withdrawnMembers)
	{
		withdraw.saveWithdrawnMembers(lap, team, currentUser, withdrawnMembers);
	}

	public void saveInputData(Lap lap, Team team, InputMode inputMode, Date checkTime,
	        String checkpoints, User currentUser)
	{
		inputData.saveInputData(lap, team, inputMode, checkTime, checkpoints, currentUser);
	}
}
