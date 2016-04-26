package ru.mmb.loggermanager.conf;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static android.content.Context.MODE_PRIVATE;

public class Configuration {

    private static final String CONFIG_PREFIX = "configuration";
    private static final String SAVE_DIR = "save.dir";
    private static final String UPDATE_LOGGERS = "update.loggers";
    private static final String UPDATE_PERIOD_MINUTES = "update.period.minutes";

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
        loadProperty(UPDATE_LOGGERS);
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

    public Set<String> getUpdateLoggers() {
        Set<String> result = new TreeSet<>();
        String updateLoggersString = configuration.getProperty(UPDATE_LOGGERS, "");
        if (updateLoggersString != null && updateLoggersString.length() > 0) {
            String[] updateLoggers = updateLoggersString.split(",");
            for (String updateLogger : updateLoggers) {
                result.add(updateLogger);
            }
        }
        return result;
    }

    public void changeLoggerState(Context context, String updateLogger, boolean checked) {
        Set<String> updateLoggers = getUpdateLoggers();
        if (checked) {
            updateLoggers.add(updateLogger);
        } else {
            updateLoggers.remove(updateLogger);
        }
        setValue(context, UPDATE_LOGGERS, buildUpdateLoggersString(updateLoggers));
    }

    private String buildUpdateLoggersString(Set<String> updateLoggers) {
        StringBuilder sb = new StringBuilder();
        for (String updateLogger : updateLoggers) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(updateLogger);
        }
        return sb.toString();
    }

    public int getUpdatePeriodMinutes() {
        String periodString = configuration.getProperty(UPDATE_PERIOD_MINUTES, "10");
        return Integer.parseInt(periodString);
    }

    public void setUpdatePeriodMinutes(Context context, int value) {
        setValue(context, UPDATE_PERIOD_MINUTES, Integer.toString(value));
    }
}
