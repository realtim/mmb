package ru.mmb.datacollector.conf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServletConfigurationAdapter extends ConfigurationAdapter {
	private static final Logger logger = LogManager.getLogger(ServletConfigurationAdapter.class);

	private ServletConfigurationAdapter() {
	}

	@Override
	public int getCurrentRaidId() {
		return Settings.getInstance().getCurrentRaidId();
	}

	public static void init() {
		ConfigurationAdapter.configurationAdapterFactory = new ServletConfigurationAdapterFactory();
		logger.info("configuration adapter initialized");
	}

	private static class ServletConfigurationAdapterFactory implements ConfigurationAdapterFactory {
		@Override
		public ConfigurationAdapter createConfigurationAdapter() {
			return new ServletConfigurationAdapter();
		}
	}
}
