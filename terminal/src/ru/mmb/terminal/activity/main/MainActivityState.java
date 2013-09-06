package ru.mmb.terminal.activity.main;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.CurrentState;
import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.registry.Settings;
import android.content.Context;
import android.os.Bundle;

public class MainActivityState extends CurrentState
{
	public MainActivityState()
	{
		super("main");
	}

	public boolean isEnabled()
	{
		if (!isDBFileSelected())
		{
			return false;
		}
		if (!isConnected())
		{
			return false;
		}
		if (!isCurrentRaidSelected())
		{
			return false;
		}
		return true;
	}

	public boolean isDBFileSelected()
	{
		String dbFileName = Settings.getInstance().getPathToTerminalDB();
		return dbFileName != null && dbFileName.length() > 0;
	}

	public String getDBFileText(Context context)
	{
		return context.getResources().getString(R.string.main_database_file)
		        + Settings.getInstance().getPathToTerminalDB();
	}

	public boolean isConnected()
	{
		return TerminalDB.getRawInstance().isConnected();
	}

	public String getConnectionText(Context context)
	{
		if (isConnected())
		{
			return context.getResources().getString(R.string.main_database_connection_OK);
		}
		else
		{
			return context.getResources().getString(R.string.main_database_connection_NOK);
		}
	}

	public boolean isCurrentRaidSelected()
	{
		return Settings.getInstance().getCurrentRaidId() != -1;
	}

	public String getCurrentRaidIDText(Context context)
	{
		String result = context.getResources().getString(R.string.main_current_raid_id);
		int raidId = Settings.getInstance().getCurrentRaidId();
		if (raidId != -1)
		{
			result += " " + raidId;
		}
		return result;
	}

	public int getColor(Context context, boolean enabled)
	{
		if (enabled)
		{
			return context.getResources().getColor(R.color.LightGreen);
		}
		else
		{
			return context.getResources().getColor(R.color.Red);
		}
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
