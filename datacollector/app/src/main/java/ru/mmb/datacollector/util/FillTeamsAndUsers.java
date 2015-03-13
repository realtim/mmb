package ru.mmb.datacollector.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.model.Team;
import android.database.sqlite.SQLiteDatabase;

public class FillTeamsAndUsers
{
	private static SQLiteDatabase db;

	public static void execute()
	{
		db = DatacollectorDB.getConnectedInstance().getDb();
		try
		{
			importTeams();
			importUsers();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static void importTeams() throws IOException
	{
		List<String> teamStrings = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = readTeamStrings(teamStrings);
			saveTeamsToDB(teamStrings);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (reader != null) reader.close();
		}
	}

	private static BufferedReader readTeamStrings(List<String> teamStrings) throws IOException
	{
		return readFileStrings(teamStrings, ExternalStorage.getDir() + "/mmb/model/teams.csv");
	}

	private static BufferedReader readFileStrings(List<String> resultStrings, String fileName)
	        throws IOException
	{
		BufferedReader reader;
		FileInputStream fis = new FileInputStream(new File(fileName));
		reader = new BufferedReader(new InputStreamReader(fis, "Cp1251"));
		String currentString = reader.readLine();
		while (currentString != null)
		{
			resultStrings.add(currentString);
			currentString = reader.readLine();
		}
		return reader;
	}

	private static void saveTeamsToDB(List<String> teamStrings)
	{
		String sql =
		    "insert into Teams(team_id, team_name, distance_id, team_num) values (?, ?, 1, ?)";
		db.beginTransaction();
		for (String teamString : teamStrings)
		{
			Team team = parseTeam(teamString);
			if (team == null) continue;
			db.execSQL(sql, new Object[] { new Integer(team.getTeamId()), team.getTeamName(),
			        new Integer(team.getTeamNum()) });
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	private static Team parseTeam(String teamString)
	{
		if (ParseUtils.isEmpty(teamString)) return null;

		String[] strings = teamString.trim().split("\\|");
		int teamId = Integer.parseInt(strings[0]);
		int distanceId = 1;
		int teamNum = Integer.parseInt(strings[2]);
		String teamName = strings[3];
		return new Team(teamId, distanceId, teamNum, teamName);
	}

	private static void importUsers() throws IOException
	{
		List<String> userStrings = new ArrayList<String>();
		BufferedReader reader = null;
		try
		{
			reader = readUserStrings(userStrings);
			saveUsersToDB(userStrings);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (reader != null) reader.close();
		}
	}

	private static BufferedReader readUserStrings(List<String> userStrings) throws IOException
	{
		return readFileStrings(userStrings, ExternalStorage.getDir()
		        + "/mmb/model/participants.csv");
	}

	private static void saveUsersToDB(List<String> userStrings)
	{
		String userNoBirthyearSql = "insert into Users(user_id, user_name) values (?, ?)";
		String userBirthyearSql =
		    "insert into Users(user_id, user_name, user_birthyear) values (?, ?, ?)";
		String teamUserSql =
		    "insert into TeamUsers(teamuser_id, user_id, team_id) values (?, ?, ?)";
		db.beginTransaction();
		for (String userString : userStrings)
		{
			UserInfo userInfo = parseUser(userString);
			if (userInfo == null) continue;
			if (userInfo.getUserBirthYear() == null)
			{
				db.execSQL(userNoBirthyearSql, new Object[] { new Integer(userInfo.getUserId()),
				        userInfo.getUserName() });
			}
			else
			{
				db.execSQL(userBirthyearSql, new Object[] { new Integer(userInfo.getUserId()),
				        userInfo.getUserName(), userInfo.getUserBirthYear() });
			}
			int localId = DatacollectorDB.getConnectedInstance().getNextId();
			db.execSQL(teamUserSql, new Object[] { new Integer(localId),
			        new Integer(userInfo.getUserId()), new Integer(userInfo.getTeamId()) });
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	private static UserInfo parseUser(String userString)
	{
		if (ParseUtils.isEmpty(userString)) return null;

		String[] strings = userString.split("\\|");
		int userId = Integer.parseInt(strings[0]);
		int teamId = Integer.parseInt(strings[1]);
		String userName = strings[2];
		Integer userBirthYear = null;
		if (!ParseUtils.isNull(strings[3])) userBirthYear = Integer.parseInt(strings[3]);

		return new UserInfo(userId, teamId, userName, userBirthYear);
	}

	private static class UserInfo
	{
		private final int userId;
		private final int teamId;
		private final String userName;
		private final Integer userBirthYear;

		public UserInfo(int userId, int teamId, String userName, Integer userBirthYear)
		{
			this.userId = userId;
			this.teamId = teamId;
			this.userName = userName;
			this.userBirthYear = userBirthYear;
		}

		public int getUserId()
		{
			return userId;
		}

		public int getTeamId()
		{
			return teamId;
		}

		public String getUserName()
		{
			return userName;
		}

		public Integer getUserBirthYear()
		{
			return userBirthYear;
		}
	}
}
