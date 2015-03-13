package ru.mmb.datacollector.model;

import java.io.Serializable;

public class Checkpoint implements Serializable, Comparable<Checkpoint>
{
	private static final long serialVersionUID = 8167279135428904030L;

	private int levelPointId;
	private int checkpointOrder;
	private String checkpointName;
	private int checkpointPenalty;

	private transient LevelPoint levelPoint = null;

	public Checkpoint()
	{
	}

	public Checkpoint(int levelPointId, int checkpointOrder, String checkpointName, int checkpointPenalty)
	{
		this.levelPointId = levelPointId;
		this.checkpointOrder = checkpointOrder;
		this.checkpointName = checkpointName;
		this.checkpointPenalty = checkpointPenalty;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public int getCheckpointOrder()
	{
		return checkpointOrder;
	}

	public String getCheckpointName()
	{
		return checkpointName;
	}

	public int getCheckpointPenalty()
	{
		return checkpointPenalty;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	public void setLevelPoint(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
	}

	@Override
	public int compareTo(Checkpoint another)
	{
		return new Integer(getCheckpointOrder()).compareTo(new Integer(another.getCheckpointOrder()));
	}
}
