package ru.mmb.terminal.model;

public enum PointType
{
	START(1),

	FINISH(2),

	TEAM_CONTROL(3);

	private final int id;

	private PointType(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return id;
	}
}
