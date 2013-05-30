package ru.mmb.terminal.test.model.history;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelPoint;

public class TestUtils
{
	private static final SimpleDateFormat FULL_DATE_FORMAT =
	    new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	public static TeamLevelPoint createTeamLevelPoint(Team team, String takenCheckpointNames,
	        Date checkedDateTime, Date recordDateTime)
	{
		TestBasicData testBasicData = TestBasicData.getInstance();
		LevelPoint levelPoint = testBasicData.getLevel().getFinishPoint();
		TeamLevelPoint result =
		    new TeamLevelPoint(team.getTeamId(), testBasicData.getUserId(), testBasicData.getDeviceId(), levelPoint.getLevelPointId(), takenCheckpointNames, checkedDateTime, recordDateTime);
		result.setTeam(team);
		result.setLevelPoint(levelPoint);
		result.initTakenCheckpoints();
		return result;
	}

	public static Date parseFullDate(String dateString) throws ParseException
	{
		return FULL_DATE_FORMAT.parse(dateString);
	}
}
