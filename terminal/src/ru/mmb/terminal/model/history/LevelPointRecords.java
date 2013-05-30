package ru.mmb.terminal.model.history;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.terminal.model.TeamLevelPoint;

public class LevelPointRecords
{
	private final TreeMap<Date, LevelPointRecord> levelPointRecords =
	    new TreeMap<Date, LevelPointRecord>();
	private final HashMap<Integer, Date> userToDate = new HashMap<Integer, Date>();

	private Date lastDate = null;
	private LevelPointRecord lastLevelPointRecord = null;

	public void put(TeamLevelPoint teamLevelPoint)
	{
		Date recordDateTime = teamLevelPoint.getRecordDateTime();
		Integer userId = teamLevelPoint.getUserId();
		if (userToDate.containsKey(userId))
		{
			if (userToDate.get(userId).after(recordDateTime))
			    throw new LevelPointRecordsException("New record update date is before existing record for the same user.");
			removeUserFromPreviousRecord(userId);
		}

		LevelPointRecord teamRecord = getOrCreate(recordDateTime);
		teamRecord.put(teamLevelPoint);
		// update accelerators
		userToDate.put(userId, recordDateTime);
		lastDate = levelPointRecords.lastKey();
		if (lastDate != null) lastLevelPointRecord = levelPointRecords.get(lastDate);
	}

	private LevelPointRecord getOrCreate(Date recordDateTime)
	{
		if (!levelPointRecords.containsKey(recordDateTime))
		{
			levelPointRecords.put(recordDateTime, new LevelPointRecord());
		}
		return levelPointRecords.get(recordDateTime);
	}

	private void removeUserFromPreviousRecord(Integer userId)
	{
		Date previousDate = userToDate.get(userId);
		if (previousDate == null) return;

		LevelPointRecord teamRecord = levelPointRecords.get(previousDate);
		if (teamRecord == null)
		    throw new LevelPointRecordsException("userToDate map corrupted. Broken reference to teamRecord.");
		teamRecord.removeByUserId(userId);
		if (teamRecord.isEmpty()) levelPointRecords.remove(previousDate);

		userToDate.remove(userId);
	}

	public LevelPointRecord getByDate(Date recordDateTime)
	{
		return levelPointRecords.get(recordDateTime);
	}

	public Set<Date> getRecordDates()
	{
		return levelPointRecords.keySet();
	}

	public int size()
	{
		return levelPointRecords.size();
	}

	public Date getDateForUser(Integer userId)
	{
		return userToDate.get(userId);
	}

	public LevelPointRecord getLastRecord()
	{
		return lastLevelPointRecord;
	}

	public Date getLastDate()
	{
		return lastDate;
	}
}
