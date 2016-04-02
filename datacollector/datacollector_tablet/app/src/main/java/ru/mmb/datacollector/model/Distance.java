package ru.mmb.datacollector.model;

import java.io.Serializable;

public class Distance implements Serializable
{
	private static final long serialVersionUID = 1645090862525665007L;

	private int distanceId;
	private int raidId;
	private String distanceName;

	public Distance()
	{
	}

	public Distance(int distanceId, int raidId, String distanceName)
	{
		this.distanceId = distanceId;
		this.raidId = raidId;
		this.distanceName = distanceName;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public int getRaidId()
	{
		return raidId;
	}

	public String getDistanceName()
	{
		return distanceName;
	}
}
