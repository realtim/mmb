package ru.mmb.datacollector.conf;

public abstract class ConfigurationAdapter {
	public static ConfigurationAdapterFactory configurationAdapterFactory = null;

	private static ConfigurationAdapter instance = null;

	public static synchronized ConfigurationAdapter getInstance() {
		if (instance == null) {
			instance = configurationAdapterFactory.createConfigurationAdapter();
		}
		return instance;
	}

	public abstract int getCurrentRaidId();
}
