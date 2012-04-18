package ru.mmb.terminal.conf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import ru.mmb.terminal.util.ExternalStorage;

public class Configuration
{
	private static Configuration instance = null;

	private int deviceId;
	private int userId;

	public static Configuration getInstance()
	{
		if (instance == null)
		{
			instance = new Configuration();
		}
		return instance;
	}

	private Configuration()
	{
		try
		{
			load();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't load configuration file.", e);
		}
	}

	private void load() throws FileNotFoundException, IOException
	{
		String confName = ExternalStorage.getDir() + "/mmb/terminal.conf";
		Properties properties = new Properties();
		properties.load(new InputStreamReader(new FileInputStream(confName), "UTF8"));
		deviceId = Integer.parseInt(properties.getProperty("device_id"));
		userId = Integer.parseInt(properties.getProperty("user_id"));
	}

	public int getDeviceId()
	{
		return deviceId;
	}

	public int getUserId()
	{
		return userId;
	}
}
