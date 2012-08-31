package ru.mmb.terminal.model.registry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import ru.mmb.terminal.db.TerminalDB;

public class Settings
{
	private static final String DEVICE_ID = "device_id";
	private static final String USER_ID = "user_id";
	private static final String CURRENT_RAID_ID = "current_raid_id";
	private static final String LAST_EXPORT_DATE = "last_export_date";
	private static final String TRANSP_USER_ID = "transp_user_id";
	private static final String TRANSP_USER_PASSWORD = "transp_user_password";
	private static final String TEAM_CLEAR_FILTER_AFTER_OK = "team_clear_filter_after_ok";
	private static final String TEAM_FAST_SELECT = "team_fast_select";

	private static Settings instance = null;

	private Properties settings = null;

	public static Settings getInstance()
	{
		if (instance == null)
		{
			instance = new Settings();
		}
		return instance;
	}

	private Settings()
	{
		refresh();
	}

	private void load() throws FileNotFoundException, IOException
	{
		settings = TerminalDB.getInstance().loadSettings();
	}

	public int getDeviceId()
	{
		return Integer.parseInt(settings.getProperty(DEVICE_ID, "-1"));
	}

	public void setDeviceId(String deviceId)
	{
		setValue(DEVICE_ID, deviceId);
	}

	private boolean setValue(String settingName, String newValue)
	{
		boolean changed = false;
		String oldValue = (String) settings.get(settingName);
		if (oldValue == null || !oldValue.equals(newValue))
		{
			settings.put(settingName, newValue);
			TerminalDB.getInstance().setSettingValue(settingName, newValue);
			changed = true;
		}
		return changed;
	}

	public int getUserId()
	{
		return Integer.parseInt(settings.getProperty(USER_ID, "-1"));
	}

	public void setUserId(String userId)
	{
		setValue(USER_ID, userId);
	}

	public int getCurrentRaidId()
	{
		return Integer.parseInt(settings.getProperty(CURRENT_RAID_ID, "-1"));
	}

	public void setCurrentRaidId(String currentRaidId)
	{
		boolean changed = setValue(CURRENT_RAID_ID, currentRaidId);
		if (changed)
		{
			DistancesRegistry.getInstance().refresh();
			TeamsRegistry.getInstance().refresh();
		}
	}

	public String getLastExportDate()
	{
		return settings.getProperty(LAST_EXPORT_DATE, "");
	}

	public void setLastExportDate(String lastExportDate)
	{
		setValue(LAST_EXPORT_DATE, lastExportDate);
	}

	public int getTranspUserId()
	{
		return Integer.parseInt(settings.getProperty(TRANSP_USER_ID, "-1"));
	}

	public void setTranspUserId(String transpUserId)
	{
		setValue(TRANSP_USER_ID, transpUserId);
	}

	public String getTranspUserPassword()
	{
		return settings.getProperty(TRANSP_USER_PASSWORD, "");
	}

	public void setTranspUserPassword(String transpUserPassword)
	{
		setValue(TRANSP_USER_PASSWORD, transpUserPassword);
	}

	public boolean isTeamClearFilterAfterOk()
	{
		return Boolean.parseBoolean(settings.getProperty(TEAM_CLEAR_FILTER_AFTER_OK, "false"));
	}

	public void setTeamClearFilterAfterOk(String teamClearFilterAfterOk)
	{
		setValue(TEAM_CLEAR_FILTER_AFTER_OK, teamClearFilterAfterOk);
	}

	public boolean isTeamFastSelect()
	{
		return Boolean.parseBoolean(settings.getProperty(TEAM_FAST_SELECT, "false"));
	}

	public void setTeamFastSelect(String teamFastSelect)
	{
		setValue(TEAM_FAST_SELECT, teamFastSelect);
	}

	public void refresh()
	{
		try
		{
			load();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't load settings.", e);
		}
	}
}
