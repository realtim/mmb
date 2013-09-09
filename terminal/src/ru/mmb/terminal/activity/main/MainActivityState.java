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
		if (!isAllNecessaryIdsSelected())
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
		String format = context.getResources().getString(R.string.main_database_file);
		String pathToDB = Settings.getInstance().getPathToTerminalDB();
		return String.format(format, pathToDB);
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

	public String getCurrentRaidIDText(Context context)
	{
		String format = context.getResources().getString(R.string.main_current_raid_id);
		int raidId = Settings.getInstance().getCurrentRaidId();
		return String.format(format, new Integer(raidId));
	}

	public String getUserIDText(Context context)
	{
		String format = context.getResources().getString(R.string.main_user_id);
		int userId = Settings.getInstance().getUserId();
		return String.format(format, new Integer(userId));
	}

	public String getDeviceIDText(Context context)
	{
		String format = context.getResources().getString(R.string.main_device_id);
		int deviceId = Settings.getInstance().getDeviceId();
		return String.format(format, new Integer(deviceId));
	}

	public boolean isAllNecessaryIdsSelected()
	{
		return Settings.getInstance().getUserId() != -1
		        && Settings.getInstance().getDeviceId() != -1
		        && Settings.getInstance().getCurrentRaidId() != -1;
	}

	public boolean isUserIdSelected()
	{
		return Settings.getInstance().getUserId() != -1;
	}

	public boolean isDeviceIdSelected()
	{
		return Settings.getInstance().getDeviceId() != -1;
	}

	public boolean isCurrentRaidIdSelected()
	{
		return Settings.getInstance().getCurrentRaidId() != -1;
	}

	public int getColor(Context context, boolean enabled)
	{
		if (enabled)
		{
			return context.getResources().getColor(R.color.LightGreen);
		}
		else
		{
			return context.getResources().getColor(R.color.LightPink);
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
