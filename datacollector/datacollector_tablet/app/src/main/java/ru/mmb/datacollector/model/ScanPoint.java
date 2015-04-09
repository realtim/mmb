package ru.mmb.datacollector.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScanPoint implements Serializable, Comparable<ScanPoint>
{
	private static final long serialVersionUID = 641583654952750698L;

	private int scanPointId;
	private int raidId;
	private String scanPointName;
	private int scanPointOrder;

	private transient Map<Integer, LevelPoint> levelPoints = null;

	public ScanPoint()
	{
	}

	public ScanPoint(int scanPointId, int raidId, String scanPointName, int scanPointOrder)
	{
		this.scanPointId = scanPointId;
		this.raidId = raidId;
		this.scanPointName = scanPointName;
		this.scanPointOrder = scanPointOrder;
	}

	public int getScanPointId()
	{
		return scanPointId;
	}

	public int getRaidId()
	{
		return raidId;
	}

	public String getScanPointName()
	{
		return scanPointName;
	}

	public int getScanPointOrder()
	{
		return scanPointOrder;
	}

	private Map<Integer, LevelPoint> getLevelPointsInstance()
	{
		if (levelPoints == null) levelPoints = new HashMap<Integer, LevelPoint>();
		return levelPoints;
	}

	public Map<Integer, LevelPoint> getLevelPoints()
	{
		return Collections.unmodifiableMap(getLevelPointsInstance());
	}

	public void addLevelPoint(LevelPoint levelPoint)
	{
		getLevelPointsInstance().put(levelPoint.getDistanceId(), levelPoint);
	}

	public LevelPoint getLevelPointByDistance(Integer distanceId)
	{
		return levelPoints.get(distanceId);
	}

	@Override
	public int compareTo(ScanPoint another)
	{
		return new Integer(getScanPointOrder()).compareTo(new Integer(another.getScanPointOrder()));
	}

	public LevelPoint getFirstLevelPoint()
	{
		for (LevelPoint levelPoint : levelPoints.values())
		{
			// Get first available level point from map.
			return levelPoint;
		}
		throw new RuntimeException("Cannot find any level point for scan point: " + this);
	}

	@Override
	public String toString()
	{
		return "ScanPoint [scanPointId=" + scanPointId + ", raidId=" + raidId + ", scanPointName="
		        + scanPointName + ", scanPointOrder=" + scanPointOrder + "]";
	}
}
