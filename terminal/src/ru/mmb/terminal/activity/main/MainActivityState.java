package ru.mmb.terminal.activity.main;

import ru.mmb.terminal.activity.CurrentState;
import android.os.Bundle;

public class MainActivityState extends CurrentState
{
	public MainActivityState()
	{
		super("main");
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
	}

	@Override
	protected void update(boolean fromSavedBundle)
	{
	}
}
