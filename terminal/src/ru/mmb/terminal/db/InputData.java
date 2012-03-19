package ru.mmb.terminal.db;

import java.util.Date;

import ru.mmb.terminal.activity.input.InputMode;
import ru.mmb.terminal.model.Lap;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.User;
import android.database.sqlite.SQLiteDatabase;

public class InputData
{
	private static final String TABLE_INPUT_DATA = "input_data";
	private static final String INPUT_DATA_LAP_ID = "lap_id";
	private static final String INPUT_DATA_INPUT_MODE = "input_mode";
	private static final String INPUT_DATA_TEAM_ID = "team_id";
	private static final String INPUT_DATA_CHECK_TIME = "check_time";
	private static final String INPUT_DATA_TAKEN_CHECKPOINTS = "taken_checkpoints";
	private static final String DIRTY = "dirty";
	private static final String USER_ID = "user_id";
	private static final String CREATION_TIME = "creation_time";

	private final SQLiteDatabase db;

	public InputData(SQLiteDatabase db)
	{
		this.db = db;
	}

	public void saveInputData(Lap lap, Team team, InputMode inputMode, Date checkTime,
	        String checkpoints, User currentUser)
	{
		db.beginTransaction();
		try
		{
			String insertSql =
			    "insert into " + TABLE_INPUT_DATA + "(" + INPUT_DATA_LAP_ID + ", "
			            + INPUT_DATA_INPUT_MODE + ", " + INPUT_DATA_TEAM_ID + ", "
			            + INPUT_DATA_CHECK_TIME + ", " + INPUT_DATA_TAKEN_CHECKPOINTS + ", "
			            + DIRTY + ", " + USER_ID + ", " + CREATION_TIME + ") values ("
			            + lap.getId() + ", '" + inputMode.name() + "', " + team.getId() + ", '"
			            + Constants.DATE_FORMAT.format(checkTime) + "', '" + checkpoints + "', 1, "
			            + currentUser.getId() + ", '" + Constants.DATE_FORMAT.format(new Date())
			            + "')";
			db.execSQL(insertSql);
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}
}
