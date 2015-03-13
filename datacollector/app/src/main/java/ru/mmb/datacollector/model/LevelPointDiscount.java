package ru.mmb.datacollector.model;

import java.io.Serializable;

public class LevelPointDiscount implements Serializable
{
	private static final long serialVersionUID = -382080338533329219L;

	private int levelPointDiscountId;
	private int distanceId;
	private int levelPointDiscountValue;
	private int levelPointDiscountStart;
	private int levelPointDiscountFinish;

	public LevelPointDiscount()
	{
	}

	public LevelPointDiscount(int levelPointDiscountId, int distanceId, int levelPointDiscountValue, int levelPointDiscountStart, int levelPointDiscountFinish)
	{
		this.levelPointDiscountId = levelPointDiscountId;
		this.distanceId = distanceId;
		this.levelPointDiscountValue = levelPointDiscountValue;
		this.levelPointDiscountStart = levelPointDiscountStart;
		this.levelPointDiscountFinish = levelPointDiscountFinish;
	}

	public int getLevelPointDiscountId()
	{
		return levelPointDiscountId;
	}

	public int getDistanceId()
	{
		return distanceId;
	}

	public int getLevelPointDiscountValue()
	{
		return levelPointDiscountValue;
	}

	public int getLevelPointDiscountStart()
	{
		return levelPointDiscountStart;
	}

	public int getLevelPointDiscountFinish()
	{
		return levelPointDiscountFinish;
	}

	public boolean contains(Checkpoint checkpoint)
	{
		return levelPointDiscountStart <= checkpoint.getCheckpointOrder()
		        && levelPointDiscountFinish >= checkpoint.getCheckpointOrder();
	}
}
