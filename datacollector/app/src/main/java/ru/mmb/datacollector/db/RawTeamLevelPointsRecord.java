package ru.mmb.datacollector.db;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.util.DateFormat;

public class RawTeamLevelPointsRecord implements Comparable<RawTeamLevelPointsRecord>
{
	private final Date recordDateTime;
	private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<Integer, Boolean>();

	public RawTeamLevelPointsRecord(String recordDateTime, String takenCheckpoints, ScanPoint scanPoint, Team team)
	{
		this.recordDateTime = DateFormat.parse(recordDateTime);
        LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
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

	public Map<Integer, Boolean> getCheckedMap()
	{
		return Collections.unmodifiableMap(checkedMap);
	}

	@Override
	public int compareTo(RawTeamLevelPointsRecord toCompare)
	{
		return this.recordDateTime.compareTo(toCompare.recordDateTime);
	}
}
