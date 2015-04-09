package ru.mmb.datacollector.model.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.PointType;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.DistancesRegistry;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;

public class LevelsRegistry
{
    private static LevelsRegistry instance = null;

	private static final Map<Integer, List<Level>> levels = new HashMap<Integer, List<Level>>();
	private static final Map<Integer, Level> levelsByLevelPoints = new HashMap<Integer, Level>();

    public static LevelsRegistry getInstance()
    {
        if (instance == null)
        {
            instance = new LevelsRegistry();
        }
        return instance;
    }

    private LevelsRegistry()
    {
        refresh();
    }

	public void refresh()
	{
		levels.clear();
		levelsByLevelPoints.clear();

		List<Distance> distances = DistancesRegistry.getInstance().getDistances();
        List<ScanPoint> scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
		for (Distance distance : distances)
		{
			int distanceId = distance.getDistanceId();
			List<Level> distanceLevels = buildDistanceLevels(distanceId, scanPoints);
			levels.put(distanceId, distanceLevels);
		}
	}

	private List<Level> buildDistanceLevels(int distanceId, List<ScanPoint> scanPoints)
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

	public List<Level> getLevels(int distanceId)
	{
		return levels.get(distanceId);
	}

	public Level getLevelByLevelPointId(int levelPointId)
	{
		return levelsByLevelPoints.get(levelPointId);
	}
}
