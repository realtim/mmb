package ru.mmb.datacollector.activity;

import ru.mmb.datacollector.R;

public enum LevelPointType
{
	START(R.string.global_input_mode_start, 1),

	FINISH(R.string.global_input_mode_finish, 2);

	private int displayNameId;
	private int id;

	private LevelPointType(int displayNameId, int id)
	{
		this.displayNameId = displayNameId;
		this.id = id;
	}

	public int getDisplayNameId()
	{
		return displayNameId;
	}

	public int getId()
	{
		return id;
	}

	public static LevelPointType getById(int id)
	{
		for (LevelPointType inputMode : values())
		{
			if (inputMode.getId() == id) return inputMode;
		}
		throw new RuntimeException("Unknown input mode ID: " + id);
	}
}
