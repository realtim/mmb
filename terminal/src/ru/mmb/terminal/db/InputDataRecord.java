package ru.mmb.terminal.db;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.util.DateFormat;

public class InputDataRecord implements Comparable<InputDataRecord>
{
	private final Date recordDateTime;
	private final Date checkDateTime;
	private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<Integer, Boolean>();

	public InputDataRecord(String recordDateTime, String checkDateTime, String takenCheckpoints, Level level)
	{
		this.recordDateTime = DateFormat.parse(recordDateTime);
		this.checkDateTime = DateFormat.parse(checkDateTime);
		buildCheckedMap(level);
		fillCheckedMap(takenCheckpoints, level);
	}

	private void buildCheckedMap(Level level)
	{
		for (Checkpoint checkpoint : level.getCheckpoints())
		{
			checkedMap.put(checkpoint.getCheckpointOrder(), false);
		}
	}

	private void fillCheckedMap(String takenCheckpoints, Level level)
	{
		if (takenCheckpoints.length() == 0) return;

		String[] pointNames = takenCheckpoints.split(",");
		for (int i = 0; i < pointNames.length; i++)
		{
			Checkpoint checkpoint = level.getCheckpointByName(pointNames[i]);
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
	public int compareTo(InputDataRecord toCompare)
	{
		return this.recordDateTime.compareTo(toCompare.recordDateTime);
	}
}
