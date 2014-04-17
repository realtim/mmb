package ru.mmb.terminal.model;

public enum PointType
{
	START(1),

	FINISH(2),

	TEAM_CONTROL(3),

	CHANGE_MAPS(4),

	CHECKPOINT(5);

	private final int id;

	private PointType(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}

	public boolean isFinish()
	{
		return id == 2 || id == 4;
	}

	public boolean isStart()
	{
		return id == 1;
	}

	public boolean isCheckpoint()
	{
		return id == 3 || id == 5;
	}

	public static PointType getById(int pointTypeId)
	{
		for (PointType pointType : values())
		{
			if (pointType.getId() == pointTypeId)
			{
				return pointType;
			}
		}
		return null;
	}
}
