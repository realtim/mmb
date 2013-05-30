package ru.mmb.terminal.test.model.history;

import java.text.ParseException;
import java.util.List;

import junit.framework.TestCase;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelPoint;
import ru.mmb.terminal.model.history.TeamLevelPointsStorage;
import ru.mmb.terminal.util.DateFormat;

public class TeamLevelPointsStorageTest extends TestCase
{
	private TestBasicData testBasicData;
	private TeamLevelPointsStorage teamLevelPoints;

	@Override
	protected void setUp() throws Exception
	{
		TestBasicData.reset();
		testBasicData = TestBasicData.getInstance();
		testBasicData.initTeamLevelPoints();
		teamLevelPoints = new TeamLevelPointsStorage();
		fillTeamLevelPoints();
	}

	private void fillTeamLevelPoints()
	{
		for (TeamLevelPoint teamLevelPoint : testBasicData.getTeamLevelPoints())
		{
			teamLevelPoints.put(teamLevelPoint);
		}
	}

	public void testInitialTeamLevelPoints() throws ParseException
	{
		assertEquals(5, teamLevelPoints.size());
		List<TeamLevelPoint> history = teamLevelPoints.getHistory();
		assertEquals(5, history.size());

		checkTeamLevelPoint(history.get(0), 10002, 5, "20120516213200.000", "201205162110");
		checkTeamLevelPoint(history.get(1), 10002, 4, "20120516213100.000", "201205162100");
		checkTeamLevelPoint(history.get(2), 10002, 2, "20120516213000.000", "201205161905");
		checkTeamLevelPoint(history.get(3), 10001, 3, "20120516204200.000", "201205162020");
		checkTeamLevelPoint(history.get(4), 10001, 1, "20120516162000.000", "201205161400");
	}

	private void checkTeamLevelPoint(TeamLevelPoint teamLevelPoint, int userId, int teamId,
	        String recordDateString, String checkDateString) throws ParseException
	{
		assertEquals(userId, teamLevelPoint.getUserId());
		assertEquals(TestUtils.parseFullDate(recordDateString), teamLevelPoint.getRecordDateTime());
		assertEquals(teamId, teamLevelPoint.getTeamId());
		assertEquals(DateFormat.parse(checkDateString), teamLevelPoint.getCheckDateTime());
	}

	public void testEqualRecordDates() throws ParseException
	{
		testBasicData.setUserId(10003);

		Team team = testBasicData.getTeamById(5);
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205162111"), TestUtils.parseFullDate("20120516213200.000"));
		teamLevelPoints.put(teamLevelPoint);

		List<TeamLevelPoint> history = teamLevelPoints.getHistory();
		assertEquals(6, history.size());

		checkTeamLevelPoint(history.get(0), 10003, 5, "20120516213200.000", "201205162111");
		checkTeamLevelPoint(history.get(1), 10002, 5, "20120516213200.000", "201205162110");
	}

	public void testRecordsReplacedByMoreRecent() throws ParseException
	{
		testBasicData.setUserId(10001);

		Team team = testBasicData.getTeamById(3);
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП4,КП5,КП6,А1", DateFormat.parse("201205162025"), TestUtils.parseFullDate("20120516205000.000"));
		teamLevelPoints.put(teamLevelPoint);

		testBasicData.setUserId(10003);

		team = testBasicData.getTeamById(5);
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205162111"), TestUtils.parseFullDate("20120516215200.000"));
		teamLevelPoints.put(teamLevelPoint);

		List<TeamLevelPoint> history = teamLevelPoints.getHistory();
		checkTeamLevelPoint(history.get(0), 10003, 5, "20120516215200.000", "201205162111");
		checkTeamLevelPoint(history.get(3), 10001, 3, "20120516205000.000", "201205162025");
	}
}
