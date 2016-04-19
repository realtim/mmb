package ru.mmb.loggermanager.conf;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Properties;

import static android.content.Context.MODE_PRIVATE;

public class Configuration {

    private static final String CONFIG_PREFIX = "configuration";
    private static final String SAVE_DIR = "save.dir";

    private static Configuration instance = null;

    private Properties configuration = new Properties();
    private SharedPreferences preferences;

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private Configuration() {
    }

    public void loadConfiguration(Context context) {
        preferences = context.getSharedPreferences(CONFIG_PREFIX, MODE_PRIVATE);
        loadProperty(SAVE_DIR);
    }

    private void loadProperty(String propertyName) {
        String value = preferences.getString(propertyName, null);
        if (value != null) {
            configuration.put(propertyName, value);
        }
    }

    public String getSaveDir() {
        return configuration.getProperty(SAVE_DIR, "");
    }

    public void setSaveDir(Context context, String saveDir) {
        setValue(context, SAVE_DIR, saveDir);
    }

    private boolean setValue(Context context, String settingName, String newValue) {
        boolean changed = false;
        String oldValue = (String) configuration.get(settingName);
        if (oldValue == null || !oldValue.equals(newValue)) {
            configuration.put(settingName, newValue);
            saveSetting(context, settingName, newValue);
            changed = true;
        }
        return changed;
    }

    private void saveSetting(Context context, String settingName, String value) {
        preferences = context.getSharedPreferences(CONFIG_PREFIX, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(settingName, value);
        editor.commit();
    }
}
