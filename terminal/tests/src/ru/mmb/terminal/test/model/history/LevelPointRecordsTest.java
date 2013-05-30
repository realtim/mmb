package ru.mmb.terminal.test.model.history;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelPoint;
import ru.mmb.terminal.model.history.LevelPointRecord;
import ru.mmb.terminal.model.history.LevelPointRecords;
import ru.mmb.terminal.model.history.LevelPointRecordsException;
import ru.mmb.terminal.util.DateFormat;

public class LevelPointRecordsTest extends TestCase
{
	private static final boolean CHECK_SINGLE = true;

	private TestBasicData testBasicData;
	private LevelPointRecords levelPointRecords;

	@Override
	protected void setUp() throws Exception
	{
		TestBasicData.reset();
		testBasicData = TestBasicData.getInstance();
		levelPointRecords = new LevelPointRecords();
	}

	public void testAddLevelPointRecord() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);
		Date recordDateTime = TestUtils.parseFullDate("20120516162030.500");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
		Integer userId = teamLevelPoint.getUserId();

		levelPointRecords.put(teamLevelPoint);

		assertEquals(1, levelPointRecords.getRecordDates().size());

		LevelPointRecord teamRecord = levelPointRecords.getByDate(recordDateTime);
		assertNotNull(teamRecord);
		assertEquals(1, teamRecord.size());

		teamLevelPoint = teamRecord.getByUserId(userId);
		assertNotNull(teamLevelPoint);
		assertEquals(3, teamLevelPoint.getTakenCheckpoints().size());
		assertEquals(DateFormat.parse("201205161400"), teamLevelPoint.getCheckDateTime());
	}

	public void testAddLevelPointRecordTwice() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);
		Date recordDateTime = TestUtils.parseFullDate("20120516162030.500");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
		Integer userId = teamLevelPoint.getUserId();

		levelPointRecords.put(teamLevelPoint);
		assertEquals(1, levelPointRecords.getRecordDates().size());
		assertNotNull(levelPointRecords.getDateForUser(userId));
		assertEquals(recordDateTime, levelPointRecords.getDateForUser(userId));

		recordDateTime = TestUtils.parseFullDate("20120516162500.111");
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4", DateFormat.parse("201205161305"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		assertEquals(1, levelPointRecords.getRecordDates().size());
		assertNotNull(levelPointRecords.getDateForUser(userId));
		assertEquals(TestUtils.parseFullDate("20120516162500.111"), levelPointRecords.getDateForUser(userId));

		LevelPointRecord teamRecord = levelPointRecords.getByDate(recordDateTime);
		assertNotNull(teamRecord);
		assertEquals(1, teamRecord.size());

		teamLevelPoint = teamRecord.getByUserId(userId);
		assertNotNull(teamLevelPoint);
		assertEquals(3, teamLevelPoint.getTakenCheckpoints().size());
		assertTrue(teamLevelPoint.getTakenCheckpointNames().contains("КП4"));
		assertFalse(teamLevelPoint.getTakenCheckpointNames().contains("КП2"));
		assertEquals(DateFormat.parse("201205161305"), teamLevelPoint.getCheckDateTime());
	}

	public void testExceptionAddRecordBeforeExisting() throws ParseException
	{
		boolean wasError = false;
		try
		{
			Team team = testBasicData.getTeamById(1);
			Date recordDateTime = TestUtils.parseFullDate("20120516162500.111");
			TeamLevelPoint teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4", DateFormat.parse("201205161305"), recordDateTime);
			levelPointRecords.put(teamLevelPoint);

			recordDateTime = TestUtils.parseFullDate("20120516162030.500");
			teamLevelPoint =
			    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
			levelPointRecords.put(teamLevelPoint);
		}
		catch (LevelPointRecordsException e)
		{
			wasError = true;
		}
		assertTrue("Expected [TeamRecordsException] not thrown", wasError);
	}

	public void testAddRecordsFromDifferentUsers() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);

		int userId1 = 10001;
		testBasicData.setUserId(userId1);
		Date recordDateTime1 = TestUtils.parseFullDate("20120516162030.500");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime1);
		levelPointRecords.put(teamLevelPoint);

		int userId2 = 10002;
		testBasicData.setUserId(userId2);
		Date recordDateTime2 = TestUtils.parseFullDate("20120516162500.111");
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4,КП5", DateFormat.parse("201205161305"), recordDateTime2);
		levelPointRecords.put(teamLevelPoint);

		assertEquals(2, levelPointRecords.getRecordDates().size());

		checkUserDate(userId1, "20120516162030.500");
		checkUserDate(userId2, "20120516162500.111");

		checkRecordForUser(userId1, recordDateTime1, "201205161400", 3, CHECK_SINGLE);
		checkRecordForUser(userId2, recordDateTime2, "201205161305", 4, CHECK_SINGLE);
	}

	private void checkUserDate(int userId1, String dateString) throws ParseException
	{
		assertNotNull(levelPointRecords.getDateForUser(userId1));
		assertEquals(TestUtils.parseFullDate(dateString), levelPointRecords.getDateForUser(userId1));
	}

	private void checkRecordForUser(int userId, Date recordDateTime, String checkDateString,
	        int takenSize, boolean checkRecordSingle)
	{
		LevelPointRecord teamRecord = levelPointRecords.getByDate(recordDateTime);
		assertNotNull(teamRecord);
		if (checkRecordSingle) assertEquals(1, teamRecord.size());
		TeamLevelPoint teamLevelPoint = teamRecord.getByUserId(userId);
		assertEquals(takenSize, teamLevelPoint.getTakenCheckpoints().size());
		assertEquals(DateFormat.parse(checkDateString), teamLevelPoint.getCheckDateTime());
	}

	public void testAddRecordsFromDifferentUsersToSameTime() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);

		int userId1 = 10001;
		testBasicData.setUserId(userId1);
		Date recordDateTime = TestUtils.parseFullDate("20120516162000.000");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		int userId2 = 10002;
		testBasicData.setUserId(userId2);
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4,КП5", DateFormat.parse("201205161305"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		assertEquals(1, levelPointRecords.getRecordDates().size());

		checkUserDate(userId1, "20120516162000.000");
		checkUserDate(userId2, "20120516162000.000");

		checkRecordForUser(userId1, recordDateTime, "201205161400", 3, !CHECK_SINGLE);
		checkRecordForUser(userId2, recordDateTime, "201205161305", 4, !CHECK_SINGLE);
	}

	public void testGetLastRecordSingle() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);

		int userId1 = 10001;
		testBasicData.setUserId(userId1);
		Date recordDateTime1 = TestUtils.parseFullDate("20120516162030.500");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime1);
		levelPointRecords.put(teamLevelPoint);

		int userId2 = 10002;
		testBasicData.setUserId(userId2);
		Date recordDateTime2 = TestUtils.parseFullDate("20120516162500.111");
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4,КП5", DateFormat.parse("201205161305"), recordDateTime2);
		levelPointRecords.put(teamLevelPoint);

		LevelPointRecord teamRecord = levelPointRecords.getLastRecord();
		assertNotNull(teamRecord);
		assertEquals(1, teamRecord.size());

		List<TeamLevelPoint> teamLevelPoints = teamRecord.getTeamLevelPoints();
		assertNotNull(teamLevelPoints);
		assertEquals(1, teamLevelPoints.size());
		teamLevelPoint = teamLevelPoints.get(0);
		assertEquals(recordDateTime2, teamLevelPoint.getRecordDateTime());
		assertEquals(4, teamLevelPoint.getTakenCheckpoints().size());
		assertEquals(DateFormat.parse("201205161305"), teamLevelPoint.getCheckDateTime());
	}

	public void testGetLastRecordMultiple() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);

		int userId1 = 10001;
		testBasicData.setUserId(userId1);
		Date recordDateTime = TestUtils.parseFullDate("20120516162000.000");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		int userId2 = 10002;
		testBasicData.setUserId(userId2);
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4,КП5", DateFormat.parse("201205161305"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		LevelPointRecord teamRecord = levelPointRecords.getLastRecord();
		assertNotNull(teamRecord);
		assertEquals(2, teamRecord.size());

		List<TeamLevelPoint> teamLevelPoints = teamRecord.getTeamLevelPoints();
		assertEquals(2, teamLevelPoints.size());
		teamLevelPoint = teamLevelPoints.get(0);
		checkRecord(teamLevelPoint, userId1, "201205161400", 3);
		teamLevelPoint = teamLevelPoints.get(1);
		checkRecord(teamLevelPoint, userId2, "201205161305", 4);
	}

	private void checkRecord(TeamLevelPoint record, int userId, String checkDateString,
	        int takenSize)
	{
		assertNotNull(record);
		assertEquals(userId, record.getUserId());
		assertEquals(takenSize, record.getTakenCheckpoints().size());
		assertEquals(DateFormat.parse(checkDateString), record.getCheckDateTime());
	}

	public void testAddRecordsMigration() throws ParseException
	{
		Team team = testBasicData.getTeamById(1);

		int userId1 = 10001;
		testBasicData.setUserId(userId1);
		Date recordDateTime = TestUtils.parseFullDate("20120516162000.000");
		TeamLevelPoint teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП2,КП3", DateFormat.parse("201205161400"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		int userId2 = 10002;
		testBasicData.setUserId(userId2);
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1,КП3,КП4,КП5", DateFormat.parse("201205161305"), recordDateTime);
		levelPointRecords.put(teamLevelPoint);

		assertEquals(1, levelPointRecords.size());
		LevelPointRecord teamRecord = levelPointRecords.getByDate(recordDateTime);
		assertEquals(2, teamRecord.size());

		Date recordDateTime2 = TestUtils.parseFullDate("20120516162559.123");
		teamLevelPoint =
		    TestUtils.createTeamLevelPoint(team, "КП1", DateFormat.parse("201205161200"), recordDateTime2);
		levelPointRecords.put(teamLevelPoint);

		assertEquals(2, levelPointRecords.size());

		teamRecord = levelPointRecords.getByDate(recordDateTime);
		assertEquals(1, teamRecord.size());
		assertEquals(recordDateTime, levelPointRecords.getDateForUser(userId1));
		teamRecord = levelPointRecords.getByDate(recordDateTime2);
		assertEquals(1, teamRecord.size());
		assertEquals(recordDateTime2, levelPointRecords.getDateForUser(userId2));

		assertEquals(recordDateTime2, levelPointRecords.getLastDate());
		teamRecord = levelPointRecords.getLastRecord();
		teamLevelPoint = teamRecord.getTeamLevelPoints().get(0);
		checkRecord(teamLevelPoint, userId2, "201205161200", 1);
	}
}
