package ru.mmb.terminal.model.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.PointType;
import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.registry.DistancesRegistry;

public class LevelsRegistry
{
	private static final Map<Integer, List<Level>> levels = new HashMap<Integer, List<Level>>();
	private static final Map<Integer, Level> levelsByLevelPoints = new HashMap<Integer, Level>();

	public static void buildLevels(List<ScanPoint> scanPoints)
	{
		levels.clear();
		levelsByLevelPoints.clear();

		List<Distance> distances = DistancesRegistry.getInstance().getDistances();
		for (Distance distance : distances)
		{
			int distanceId = distance.getDistanceId();
			List<Level> distanceLevels = buildDistanceLevels(distanceId, scanPoints);
			levels.put(distanceId, distanceLevels);
		}
	}

	private static List<Level> buildDistanceLevels(int distanceId, List<ScanPoint> scanPoints)
	{
		List<Level> result = new ArrayList<Level>();
		Level level = null;
		for (ScanPoint scanPoint : scanPoints)
		{
			LevelPoint levelPoint = scanPoint.getLevelPointByDistance(distanceId);
			if (levelPoint != null)
			{
				if (levelPoint.getPointType() == PointType.START)
				{
					level = new Level();
				}
				level.addLevelPoint(levelPoint);
				levelsByLevelPoints.put(levelPoint.getLevelPointId(), level);
				if (levelPoint.getPointType() == PointType.FINISH)
				{
					result.add(level);
				}
			}
		}
		return result;
	}

	public static List<Level> getLevels(int distanceId)
	{
		return levels.get(distanceId);
	}

	public static Level getLevelByLevelPointId(int levelPointId)
	{
		return levelsByLevelPoints.get(levelPointId);
	}
}
