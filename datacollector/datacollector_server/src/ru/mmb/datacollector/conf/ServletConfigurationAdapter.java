package ru.mmb.datacollector.conf;

public class ServletConfigurationAdapter extends ConfigurationAdapter {
	private ServletConfigurationAdapter() {
	}

	@Override
	public int getCurrentRaidId() {
		return Settings.getInstance().getCurrentRaidId();
	}

	public static void init() {
		ConfigurationAdapter.configurationAdapterFactory = new ServletConfigurationAdapterFactory();
	}

	private static class ServletConfigurationAdapterFactory implements ConfigurationAdapterFactory {
		@Override
		public ConfigurationAdapter createConfigurationAdapter() {
			return new ServletConfigurationAdapter();
		}
	}
}
