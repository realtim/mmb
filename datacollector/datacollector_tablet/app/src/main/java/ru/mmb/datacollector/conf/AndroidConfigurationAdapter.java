package ru.mmb.datacollector.conf;

import android.util.Log;

import ru.mmb.datacollector.model.registry.Settings;

public class AndroidConfigurationAdapter extends ConfigurationAdapter {
    private AndroidConfigurationAdapter() {
    }

    @Override
    public int getCurrentRaidId() {
        return Settings.getInstance().getCurrentRaidId();
    }

    public static void init() {
        ConfigurationAdapter.configurationAdapterFactory = new AndroidConfigurationAdapterFactory();
        Log.d("CONF_ADAPTER", "initialized");
    }

    private static class AndroidConfigurationAdapterFactory implements ConfigurationAdapterFactory {
        @Override
        public ConfigurationAdapter createConfigurationAdapter() {
            return new AndroidConfigurationAdapter();
        }
    }
}
