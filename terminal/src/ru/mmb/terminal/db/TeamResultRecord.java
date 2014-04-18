package ru.mmb.terminal.db;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.util.DateFormat;

public class TeamResultRecord implements Comparable<TeamResultRecord>
{
	private final Date recordDateTime;
	private final Date checkDateTime;
	private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<Integer, Boolean>();

	public TeamResultRecord(String recordDateTime, String checkDateTime, String takenCheckpoints, LevelPoint levelPoint)
	{
		this.recordDateTime = DateFormat.parse(recordDateTime);
		this.checkDateTime = DateFormat.parse(checkDateTime);

		if (levelPoint.getPointType().isFinish())
		{
			buildCheckedMap(levelPoint);
			fillCheckedMap(takenCheckpoints, levelPoint);
		}
	}

	private void buildCheckedMap(LevelPoint levelPoint)
	{
		for (Checkpoint checkpoint : levelPoint.getCheckpoints())
		{
			checkedMap.put(checkpoint.getCheckpointOrder(), false);
		}
	}

	private void fillCheckedMap(String takenCheckpoints, LevelPoint levelPoint)
	{
		if (takenCheckpoints.length() == 0) return;

		String[] pointNames = takenCheckpoints.split(",");
		for (int i = 0; i < pointNames.length; i++)
		{
			Checkpoint checkpoint = levelPoint.getCheckpointByName(pointNames[i]);
			checkedMap.put(checkpoint.getCheckpointOrder(), true);
		}
	}

	public Date getCheckDateTime()
	{
		return checkDateTime;
	}

	public Map<Integer, Boolean> getCheckedMap()
	{
		return Collections.unmodifiableMap(checkedMap);
	}

	@Override
	public int compareTo(TeamResultRecord toCompare)
	{
		return this.recordDateTime.compareTo(toCompare.recordDateTime);
	}
}
