package ru.mmb.terminal.model;

import java.io.Serializable;

public class LevelPoint implements Serializable
{
	private static final long serialVersionUID = -7882002727049857975L;

	private final PointType pointType;
	private final int levelPointId;
	private final int levelId;

	transient private Level level = null;

	public LevelPoint(PointType pointType, int levelPointId, int levelId)
	{
		this.pointType = pointType;
		this.levelPointId = levelPointId;
		this.levelId = levelId;
	}

	public PointType getPointType()
	{
		return pointType;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public int getLevelId()
	{
		return levelId;
	}

	public Level getLevel()
	{
		return level;
	}

	public void setLevel(Level level)
	{
		this.level = level;
	}
}
