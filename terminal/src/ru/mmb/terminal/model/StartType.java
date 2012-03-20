package ru.mmb.terminal.model;

public enum StartType
{
	WHEN_READY(1),

	COMMON_START(2),

	USE_PREVIOUS_FINISH(3);

	private int typeId;

	private StartType(int typeId)
	{
		this.typeId = typeId;
	}

	public int getTypeId()
	{
		return typeId;
	}

	public static StartType getTypeById(int typeId)
	{
		for (StartType startType : values())
		{
			if (startType.getTypeId() == typeId) return startType;
		}
		throw new RuntimeException("Unknown start type ID: " + typeId);
	}
}
