package ru.mmb.terminal.test.model.history;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.PointType;
import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamResult;
import ru.mmb.terminal.util.DateFormat;

public class TestBasicData
{
	private static TestBasicData instance = null;

	public static TestBasicData getInstance()
	{
		if (instance == null)
		{
			instance = new TestBasicData();
		}
		return instance;
	}

	public static void reset()
	{
		instance = new TestBasicData();
	}

	private final int currentRaidId = 1;
	private int userId = 1;
	private final int deviceId = 1;

	private final Distance distance;
	private final ScanPoint scanPoint;
	private final LevelPoint levelPoint;

	private final List<Team> teams = new ArrayList<Team>();

	private final List<TeamResult> teamLevelPoints = new ArrayList<TeamResult>();

	private TestBasicData()
	{
		scanPoint = new ScanPoint(8, 25, "Смена карт", 2);
		distance = new Distance(1, 1, "Тестовая дистанция");
		levelPoint = createLevelPoint(distance, scanPoint);
		initTeams();
	}

	private LevelPoint createLevelPoint(Distance distance, ScanPoint scanPoint)
	{
		LevelPoint result =
		    new LevelPoint(154, PointType.CHANGE_MAPS, 1, 8, 7, DateFormat.parse("201404010100"), DateFormat.parse("201404012359"));
		List<String> levelPointNames =
		    Arrays.asList(new String[] { "КП1", "КП2", "КП3", "КП4", "КП5" });
		List<Integer> levelPointPenalties = Arrays.asList(new Integer[] { 120, 120, 120, 120, 60 });
		result.addCheckpoints(levelPointNames, levelPointPenalties);
		result.setDistance(distance);
		result.setScanPoint(scanPoint);

		return result;
	}

	private void initTeams()
	{
		Team team = new Team(1, 1, 1, "Команда 1");
		addMemberToTeam(team, 2, 1001, "User 1");
		addMemberToTeam(team, 3, 1002, "User 2");
		addMemberToTeam(team, 4, 1003, "User 3");
		teams.add(team);

		team = new Team(2, 1, 2, "Команда 2");
		addMemberToTeam(team, 5, 1004, "User 5");
		teams.add(team);

		team = new Team(3, 1, 3, "Команда 3");
		addMemberToTeam(team, 6, 1005, "User 6");
		addMemberToTeam(team, 7, 1006, "User 7");
		teams.add(team);

		team = new Team(4, 1, 4, "Команда 4");
		addMemberToTeam(team, 8, 1007, "User 8");
		addMemberToTeam(team, 9, 1008, "User 9");
		teams.add(team);

		team = new Team(5, 1, 5, "Команда 5");
		addMemberToTeam(team, 10, 1009, "User 10");
		teams.add(team);
	}

	private void addMemberToTeam(Team team, int userId, int teamUserId, String userName)
	{
		Participant member = new Participant(userId, team.getTeamId(), teamUserId, userName);
		team.addMember(member);
		member.setTeam(team);
	}

	public void initTeamLevelPoints()
	{
		try
		{
			userId = 10001;

			Team team = getTeamById(1);
			TeamResult teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), TestUtils.parseFullDate("20120516162000.000"));
			teamLevelPoints.add(teamLevelPoint);

			team = getTeamById(2);
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3,КП4,КП5,КП6", DateFormat.parse("201205161805"), TestUtils.parseFullDate("20120516182100.000"));
			teamLevelPoints.add(teamLevelPoint);

			team = getTeamById(3);
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП4,КП5,КП6,А1", DateFormat.parse("201205162020"), TestUtils.parseFullDate("20120516204200.000"));
			teamLevelPoints.add(teamLevelPoint);

			userId = 10002;

			team = getTeamById(2);
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3,КП4,КП5,КП6", DateFormat.parse("201205161905"), TestUtils.parseFullDate("20120516213000.000"));
			teamLevelPoints.add(teamLevelPoint);

			team = getTeamById(4);
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205162100"), TestUtils.parseFullDate("20120516213100.000"));
			teamLevelPoints.add(teamLevelPoint);

			team = getTeamById(5);
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205162110"), TestUtils.parseFullDate("20120516213200.000"));
			teamLevelPoints.add(teamLevelPoint);
		}
		catch (ParseException e)
		{
		}
	}

	public int getCurrentRaidId()
	{
		return currentRaidId;
	}

	public int getUserId()
	{
		return userId;
	}

	public int getDeviceId()
	{
		return deviceId;
	}

	public List<Team> getTeams()
	{
		return teams;
	}

	public List<TeamResult> getTeamLevelPoints()
	{
		return teamLevelPoints;
	}

	public Team getTeamById(int teamId)
	{
		for (Team team : teams)
		{
			if (team.getTeamId() == teamId) return team;
		}
		return null;
	}

	public void setUserId(int userId)
	{
		this.userId = userId;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	public ScanPoint getScanPoint()
	{
		return scanPoint;
	}
}
