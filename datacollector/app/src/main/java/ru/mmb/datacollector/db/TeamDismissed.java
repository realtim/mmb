package ru.mmb.datacollector.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamDismiss;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.model.registry.UsersRegistry;
import ru.mmb.datacollector.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TeamDismissed
{
	private static final String TABLE_DISMISS = "TeamLevelDismiss";
	private static final String TABLE_LEVELPOINTS = "LevelPoints";

	private static final String DISMISS_DATE = "teamleveldismiss_date";
	private static final String DEVICE_ID = "device_id";
	private static final String USER_ID = "user_id";
	private static final String TEAM_ID = "team_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String LEVELPOINT_ORDER = "levelpoint_order";
	private static final String TEAMUSER_ID = "teamuser_id";
	private static final String DISTANCE_ID = "distance_id";

	private static final String TEMPLATE_TEAMUSER_ID = "%teamuser_id%";

	private final SQLiteDatabase db;

	public TeamDismissed(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<Participant> getDismissedMembers(LevelPoint levelPoint, Team team)
	{
		List<Participant> result = new ArrayList<Participant>();
		String sql =
		    "select distinct d." + TEAMUSER_ID + ", lp." + LEVELPOINT_ID + ", lp."
		            + LEVELPOINT_ORDER + " from " + TABLE_DISMISS + " as d join "
		            + TABLE_LEVELPOINTS + " as lp on (d." + LEVELPOINT_ID + " = lp."
		            + LEVELPOINT_ID + ") where d." + TEAM_ID + " = " + team.getTeamId()
		            + " and lp." + LEVELPOINT_ORDER + " <= " + levelPoint.getLevelPointOrder();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int participantId = resultCursor.getInt(0);
			int levelPointId = resultCursor.getInt(1);
			int levelPointOrder = resultCursor.getInt(2);
			if (isDBLevelPointEarlier(levelPoint, levelPointId, levelPointOrder))
			{
				Participant member = team.getMember(participantId);
				if (!result.contains(member))
				{
					result.add(member);
				}
			}
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	private boolean isDBLevelPointEarlier(LevelPoint currLevelPoint, int dbLevelPointId,
	        int dbLevelOrder)
	{
		if (currLevelPoint.getLevelPointOrder() > dbLevelOrder) return true;
		if (currLevelPoint.getPointType().isFinish()) return true;
		if (currLevelPoint.getPointType().isStart()
		        && currLevelPoint.getLevelPointId() == dbLevelPointId) return true;
		return false;
	}

	public void saveDismissedMembers(LevelPoint levelPoint, Team team,
	        List<Participant> dismissedMembers, Date recordDateTime)
	{
		String selectSql =
		    "select count(*) from " + TABLE_DISMISS + " where " + USER_ID + " = "
		            + Settings.getInstance().getUserId() + " and " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId() + " and " + TEAM_ID + " = " + team.getTeamId()
		            + " and " + TEAMUSER_ID + " = " + TEMPLATE_TEAMUSER_ID;
		String insertSql =
		    "insert into " + TABLE_DISMISS + "(" + DISMISS_DATE + ", " + DEVICE_ID + ", " + USER_ID
		            + ", " + LEVELPOINT_ID + ", " + TEAM_ID + ", " + TEAMUSER_ID + ") values (?, "
		            + Settings.getInstance().getDeviceId() + ", "
		            + Settings.getInstance().getUserId() + ", " + levelPoint.getLevelPointId()
		            + ", " + team.getTeamId() + ", ?)";
		db.beginTransaction();
		try
		{
			for (Participant member : dismissedMembers)
			{
				if (isRecordExists(selectSql, member)) continue;
				db.execSQL(insertSql, new Object[] { DateFormat.format(recordDateTime),
				        new Integer(member.getUserId()) });
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	private boolean isRecordExists(String selectSql, Participant member)
	{
		Cursor resultCursor =
		    db.rawQuery(selectSql.replace(TEMPLATE_TEAMUSER_ID, Integer.toString(member.getUserId())), null);
		resultCursor.moveToFirst();
		int recordCount = resultCursor.getInt(0);
		resultCursor.close();
		return recordCount > 0;
	}

	public List<TeamDismiss> loadDismissedMembers(LevelPoint levelPoint)
	{
		List<TeamDismiss> result = new ArrayList<TeamDismiss>();
		Integer distanceId = levelPoint.getDistanceId();
		String sql =
		    "select distinct d." + DISMISS_DATE + ", d." + TEAM_ID + ", d." + TEAMUSER_ID + ", lp."
		            + LEVELPOINT_ID + ", lp." + LEVELPOINT_ORDER + " from " + TABLE_DISMISS
		            + " as d join " + TABLE_LEVELPOINTS + " as lp on (d." + LEVELPOINT_ID
		            + " = lp." + LEVELPOINT_ID + ") where lp." + DISTANCE_ID + " = " + distanceId
		            + " and lp." + LEVELPOINT_ORDER + " <= " + levelPoint.getLevelPointOrder();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
			int teamId = resultCursor.getInt(1);
			int teamUserId = resultCursor.getInt(2);
			int levelPointId = resultCursor.getInt(3);
			int levelPointOrder = resultCursor.getInt(4);
			if (isDBLevelPointEarlier(levelPoint, levelPointId, levelPointOrder))
			{
				TeamDismiss teamDismiss =
				    new TeamDismiss(levelPoint.getScanPoint().getScanPointId(), teamId, teamUserId, recordDateTime);

				// init reference fields
				teamDismiss.setScanPoint(levelPoint.getScanPoint());
				teamDismiss.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
				teamDismiss.setTeamUser(UsersRegistry.getInstance().getUserById(teamUserId));

				result.add(teamDismiss);
			}
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public void appendLevelPointTeams(LevelPoint levelPoint, Set<Integer> teams)
	{
		String sql =
		    "select distinct " + TEAM_ID + " from " + TABLE_DISMISS + " where " + LEVELPOINT_ID
		            + " = " + levelPoint.getLevelPointId();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			teams.add(resultCursor.getInt(0));
			resultCursor.moveToNext();
		}
		resultCursor.close();
	}
}
