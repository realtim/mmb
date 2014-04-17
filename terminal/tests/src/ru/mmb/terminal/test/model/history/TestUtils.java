package ru.mmb.terminal.test.model.history;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamResult;

public class TestUtils
{
	private static final SimpleDateFormat FULL_DATE_FORMAT =
	    new SimpleDateFormat("yyyyMMddHHmmss.SSS");

	public static TeamResult createTeamLevelPoint(Team team, String takenCheckpointNames,
	        Date checkedDateTime, Date recordDateTime)
	{
		TestBasicData testBasicData = TestBasicData.getInstance();
		ScanPoint scanPoint = testBasicData.getScanPoint();
		TeamResult result =
		    new TeamResult(team.getTeamId(), testBasicData.getUserId(), testBasicData.getDeviceId(), scanPoint.getScanPointId(), takenCheckpointNames, checkedDateTime, recordDateTime);
		result.setTeam(team);
		result.setScanPoint(scanPoint);
		result.initTakenCheckpoints();
		return result;
	}

	public static Date parseFullDate(String dateString) throws ParseException
	{
		return FULL_DATE_FORMAT.parse(dateString);
	}
}
