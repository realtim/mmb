package ru.mmb.terminal.model.history;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.terminal.model.TeamResult;

public class ScanPointRecords
{
	private final TreeMap<Date, ScanPointRecord> scanPointRecords =
	    new TreeMap<Date, ScanPointRecord>();
	private final HashMap<Integer, Date> userToDate = new HashMap<Integer, Date>();

	private Date lastDate = null;
	private ScanPointRecord lastScanPointRecord = null;

	public void put(TeamResult teamResult)
	{
		Date recordDateTime = teamResult.getRecordDateTime();
		Integer userId = teamResult.getUserId();
		if (userToDate.containsKey(userId))
		{
			if (userToDate.get(userId).after(recordDateTime))
			    throw new ScanPointRecordsException("New record update date is before existing record for the same user.");
			removeUserFromPreviousRecord(userId);
		}

		ScanPointRecord teamRecord = getOrCreate(recordDateTime);
		teamRecord.put(teamResult);
		// update accelerators
		userToDate.put(userId, recordDateTime);
		lastDate = scanPointRecords.lastKey();
		if (lastDate != null) lastScanPointRecord = scanPointRecords.get(lastDate);
	}

	private ScanPointRecord getOrCreate(Date recordDateTime)
	{
		if (!scanPointRecords.containsKey(recordDateTime))
		{
			scanPointRecords.put(recordDateTime, new ScanPointRecord());
		}
		return scanPointRecords.get(recordDateTime);
	}

	private void removeUserFromPreviousRecord(Integer userId)
	{
		Date previousDate = userToDate.get(userId);
		if (previousDate == null) return;

		ScanPointRecord teamRecord = scanPointRecords.get(previousDate);
		if (teamRecord == null)
		    throw new ScanPointRecordsException("userToDate map corrupted. Broken reference to teamRecord.");
		teamRecord.removeByUserId(userId);
		if (teamRecord.isEmpty()) scanPointRecords.remove(previousDate);

		userToDate.remove(userId);
	}

	public ScanPointRecord getByDate(Date recordDateTime)
	{
		return scanPointRecords.get(recordDateTime);
	}

	public Set<Date> getRecordDates()
	{
		return scanPointRecords.keySet();
	}

	public int size()
	{
		return scanPointRecords.size();
	}

	public Date getDateForUser(Integer userId)
	{
		return userToDate.get(userId);
	}

	public ScanPointRecord getLastRecord()
	{
		return lastScanPointRecord;
	}

	public Date getLastDate()
	{
		return lastDate;
	}
}
