package ru.mmb.terminal.activity.input;

import ru.mmb.terminal.R;

public enum InputMode
{
	START(R.string.global_input_mode_start, 1),

	FINISH(R.string.global_input_mode_finish, 2);

	private int displayNameId;
	private int id;

	private InputMode(int displayNameId, int id)
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

	public static InputMode getById(int id)
	{
		for (InputMode inputMode : values())
		{
			if (inputMode.getId() == id) return inputMode;
		}
		throw new RuntimeException("Unknown input mode ID: " + id);
	}
}
