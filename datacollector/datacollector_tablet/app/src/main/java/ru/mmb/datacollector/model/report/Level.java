package ru.mmb.datacollector.model.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.PointType;

public class Level
{
	private final List<LevelPoint> levelPoints = new ArrayList<LevelPoint>();
	private LevelPoint startPoint;
	private LevelPoint finishPoint;

	public void addLevelPoint(LevelPoint levelPoint)
	{
		levelPoints.add(levelPoint);
		if (levelPoint.getPointType() == PointType.START)
		{
			startPoint = levelPoint;
		}
		else if (levelPoint.getPointType() == PointType.FINISH)
		{
			finishPoint = levelPoint;
		}
	}

	public LevelPoint getFinishPoint()
	{
		return finishPoint;
	}

	public List<LevelPoint> getLevelPoints()
	{
		return Collections.unmodifiableList(levelPoints);
	}

	public LevelPoint getStartPoint()
	{
		return startPoint;
	}
}
