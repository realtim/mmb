package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.CurrentRaid;

public class TeamsDB
{
	private static final String TABLE_DISTANCES = "Distances";
	private static final String TABLE_TEAMS = "Teams";
	private static final String TABLE_USERS = "Users";
	private static final String TABLE_TEAMUSERS = "TeamUsers";

	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAM_NAME = "team_name";
	private static final String TEAM_NUM = "team_num";
	private static final String USER_ID = "user_id";
	private static final String USER_NAME = "user_name";
	private static final String USER_BIRTHYEAR = "user_birthyear";
	private static final String TEAMUSER_ID = "teamuser_id";
	private static final String TEAMUSER_HIDE = "teamuser_hide";

	private final SQLiteDatabase db;

	public TeamsDB(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<Team> loadTeams()
	{
		List<Team> result = new ArrayList<Team>();
		String sql =
		    "select t." + TEAM_ID + ", t." + DISTANCE_ID + ", t." + TEAM_NAME + ", t." + TEAM_NUM
		            + " from " + TABLE_TEAMS + " as t where t." + DISTANCE_ID + " in (select d."
		            + DISTANCE_ID + " from " + TABLE_DISTANCES + " as d where d." + RAID_ID + " = "
		            + CurrentRaid.getId() + ")";
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int teamId = resultCursor.getInt(0);
			int distanceId = resultCursor.getInt(1);
			String teamName = resultCursor.getString(2);
			int teamNum = resultCursor.getInt(3);
			result.add(new Team(teamId, distanceId, teamNum, teamName));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		loadTeamUsers(result);

		return result;
	}

	private void loadTeamUsers(List<Team> teams)
	{
		List<Participant> participants = new ArrayList<Participant>();
		String sql =
		    "select t." + TEAM_ID + ", u." + USER_ID + ", tu." + TEAMUSER_ID + ", u." + USER_NAME
		            + ", u." + USER_BIRTHYEAR + " from " + TABLE_USERS + " as u join "
		            + TABLE_TEAMUSERS + " as tu on (u." + USER_ID + " = tu." + USER_ID + ") join "
		            + TABLE_TEAMS + " as t on (tu." + TEAM_ID + " = t." + TEAM_ID + ") where t."
		            + DISTANCE_ID + " in (select d." + DISTANCE_ID + " from " + TABLE_DISTANCES
		            + " as d where d." + RAID_ID + " = " + CurrentRaid.getId() + ") and (tu."
		            + TEAMUSER_HIDE + " is NULL or tu." + TEAMUSER_HIDE + " = 0)";
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int teamId = resultCursor.getInt(0);
			int userId = resultCursor.getInt(1);
			int teamUserId = resultCursor.getInt(2);
			String userName = resultCursor.getString(3);
			Integer userBirthYear = null;
			if (!resultCursor.isNull(4)) userBirthYear = resultCursor.getInt(4);

			participants.add(new Participant(userId, teamId, teamUserId, userName, userBirthYear));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		putParticipantsToTeams(teams, participants);
	}

	private void putParticipantsToTeams(List<Team> teams, List<Participant> participants)
	{
		Map<Integer, Team> teamsMap = createTeamsMap(teams);
		for (Participant participant : participants)
		{
			Team team = teamsMap.get(participant.getTeamId());
			team.addMember(participant);
			participant.setTeam(team);
		}
	}

	private Map<Integer, Team> createTeamsMap(List<Team> teams)
	{
		Map<Integer, Team> result = new HashMap<Integer, Team>();
		for (Team team : teams)
		{
			result.put(team.getTeamId(), team);
		}
		return result;
	}
}
