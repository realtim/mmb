package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Withdraw
{
	private static final String TABLE_WITHDRAW = "withdraw";
	private static final String WITHDRAW_LAP_ID = "lap_id";
	private static final String WITHDRAW_TEAM_ID = "team_id";
	private static final String WITHDRAW_PARTICIPANT_ID = "participant_id";
	private static final String DIRTY = "dirty";
	private static final String CREATION_TIME = "creation_time";

	private final SQLiteDatabase db;

	public Withdraw(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<Participant> getWithdrawnMembers(Level level, Team team)
	{
		List<Participant> result = new ArrayList<Participant>();
		String whereCondition =
		    WITHDRAW_LAP_ID + " <= " + level.getLevelId() + " and " + WITHDRAW_TEAM_ID + " = "
		            + team.getTeamId();
		Cursor resultCursor =
		    db.query(TABLE_WITHDRAW, new String[] { WITHDRAW_PARTICIPANT_ID }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int participantId = resultCursor.getInt(0);
			Participant member = team.getMember(participantId);
			result.add(member);
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public void saveWithdrawnMembers(Level level, Team team, List<Participant> withdrawnMembers)
	{
		db.beginTransaction();
		try
		{
			String insertSql =
			    "insert into " + TABLE_WITHDRAW + "(" + WITHDRAW_LAP_ID + ", " + WITHDRAW_TEAM_ID
			            + ", " + WITHDRAW_PARTICIPANT_ID + ", " + DIRTY + ", " + CREATION_TIME
			            + ") values (" + level.getLevelId() + ", " + team.getTeamId()
			            + ", ?, 1, ?)";
			for (Participant member : withdrawnMembers)
			{
				db.execSQL(insertSql, new Object[] { new Integer(member.getUserId()),
				        DateFormat.format(new Date()) });
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}
}
