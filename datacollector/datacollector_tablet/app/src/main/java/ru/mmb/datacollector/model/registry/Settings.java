package ru.mmb.datacollector.model.registry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.Properties;

import ru.mmb.datacollector.conf.ConfigurationAdapter;
import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.report.LevelsRegistry;

import static android.content.Context.MODE_PRIVATE;

public class Settings {
    private static final String SETTINGS_FILE = "settings";

    private static final String PATH_TO_DB = "path_to_db";
    private static final String IMPORT_DIR = "import_dir";
    private static final String DEVICE_ID = "device_id";
    private static final String USER_ID = "user_id";
    private static final String CURRENT_RAID_ID = "current_raid_id";
    private static final String CAN_EDIT_SCANTIME = "can_edit_scantime";
    private static final String LAST_EXPORT_DATE = "last_export_date";
    private static final String TRANSP_USER_ID = "transp_user_id";
    private static final String TRANSP_USER_PASSWORD = "transp_user_password";

    private static Settings instance = null;

    private Properties settings = null;

    private Context currentContext = null;
    private boolean settingsLoaded = false;

    private SharedPreferences preferences;

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private Settings() {
    }

    public void setCurrentContext(Context currentContext) {
        this.currentContext = currentContext;
        if (!settingsLoaded) {
            refresh();
        }
    }

    public void refresh() {
        if (currentContext == null) return;

        try {
            settings = new Properties();
            loadSettings();
            settingsLoaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load settings.", e);
        }
    }

    private void loadSettings() {
        preferences = currentContext.getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE);
        loadProperty(PATH_TO_DB);
        loadProperty(IMPORT_DIR);
        loadProperty(USER_ID);
        loadProperty(DEVICE_ID);
        loadProperty(CURRENT_RAID_ID);
        loadProperty(CAN_EDIT_SCANTIME);
        loadProperty(LAST_EXPORT_DATE);
        loadProperty(TRANSP_USER_ID);
        loadProperty(TRANSP_USER_PASSWORD);
    }

    private void loadProperty(String propertyName) {
        String value = preferences.getString(propertyName, null);
        if (value != null) {
            settings.put(propertyName, value);
        }
    }

    public String getPathToDB() {
        return settings.getProperty(PATH_TO_DB, "");
    }

    public void setPathToDB(String pathToDB) {
        boolean changed = setValue(PATH_TO_DB, pathToDB);
        if (changed) {
            SQLiteDatabaseAdapter.getRawInstance().closeConnection();
            // reconnect in getInstance
            SQLiteDatabaseAdapter.getRawInstance().tryConnectToDB();
            if (SQLiteDatabaseAdapter.getRawInstance().isConnected()) {
                DistancesRegistry.getInstance().refresh();
                TeamsRegistry.getInstance().refresh();
                UsersRegistry.getInstance().refresh();
            }
        }
    }

    public String getImportDir() {
        String result = settings.getProperty(IMPORT_DIR, "");
        if ("".equals(result)) {
            result = getMMBPathFromDBFile();
        }
        // Log.d("Settings", "get import directory: " + result);
        return result;
    }

    public String getMMBPathFromDBFile() {
        String result = settings.getProperty(PATH_TO_DB, "");
        if (!"".equals(result)) {
            File dbFile = new File(result);
            result = dbFile.getParent();
        }
        // Log.d("Settings", "get path to mmb directory: " + result);
        return result;
    }

    public void onImportFileSelected(String fileName) {
        if (fileName == null) {
            setImportDir("");
            return;
        }

        File importFile = new File(fileName);
        setImportDir(importFile.getParent());
    }

    private void setImportDir(String importDir) {
        setValue(IMPORT_DIR, importDir);
    }

    public String getExportDir() {
        return getMMBPathFromDBFile();
    }

    public String getDatalogDir() {
        return getMMBPathFromDBFile() + "/datalog";
    }

    public String getDBBackupDir() {
        return getMMBPathFromDBFile() + "/db_backup";
    }

    public int getDeviceId() {
        return getIntSetting(DEVICE_ID);
    }

    public void setDeviceId(String deviceId) {
        setValue(DEVICE_ID, deviceId);
    }

    public int getUserId() {
        return getIntSetting(USER_ID);
    }

    public void setUserId(String userId) {
        setValue(USER_ID, userId);
    }

    public int getCurrentRaidId() {
        return getIntSetting(CURRENT_RAID_ID);
    }

    public void setCurrentRaidId(String currentRaidId) {
        boolean changed = setValue(CURRENT_RAID_ID, currentRaidId);
        if (changed) {
            if (SQLiteDatabaseAdapter.getConnectedInstance() != null) {
                DistancesRegistry.getInstance().refresh();
                ScanPointsRegistry.getInstance().refresh();
                TeamsRegistry.getInstance().refresh();
                UsersRegistry.getInstance().refresh();
                LevelsRegistry.getInstance().refresh();
                Log.d("SETTINGS",
                        "current raid ID: " +
                                ConfigurationAdapter.getInstance().getCurrentRaidId());
            }
        }
    }

    public String getLastExportDate() {
        return settings.getProperty(LAST_EXPORT_DATE, "");
    }

    public void setLastExportDate(String lastExportDate) {
        setValue(LAST_EXPORT_DATE, lastExportDate);
    }

    public int getTranspUserId() {
        return getIntSetting(TRANSP_USER_ID);
    }

    public void setTranspUserId(String transpUserId) {
        setValue(TRANSP_USER_ID, transpUserId);
    }

    public String getTranspUserPassword() {
        return settings.getProperty(TRANSP_USER_PASSWORD, "");
    }

    public void setTranspUserPassword(String transpUserPassword) {
        setValue(TRANSP_USER_PASSWORD, transpUserPassword);
    }

    private boolean setValue(String settingName, String newValue) {
        boolean changed = false;
        String oldValue = (String) settings.get(settingName);
        if (oldValue == null || !oldValue.equals(newValue)) {
            settings.put(settingName, newValue);
            saveSetting(settingName, newValue);
            changed = true;
        }
        return changed;
    }

    public boolean isCanEditScantime() {
        return Boolean.parseBoolean(settings.getProperty(CAN_EDIT_SCANTIME, Boolean.FALSE.toString()));
    }

    public void setCanEditScantime(boolean canEditScantime) {
        setValue(CAN_EDIT_SCANTIME, Boolean.toString(canEditScantime));
    }

    private void saveSetting(String settingName, String value) {
        if (currentContext == null) {
            throw new RuntimeException("Error. Settings saveSetting while current context is NULL.");
        }

        preferences = currentContext.getSharedPreferences(SETTINGS_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(settingName, value);
        editor.commit();
    }

    private int getIntSetting(String settingName) {
        String valueString = settings.getProperty(settingName);
        if (valueString == null || valueString.trim().length() == 0) {
            return -1;
        }
        return Integer.parseInt(valueString);
    }

    /* http transport is not removed totally, so save STUB methods to compile code */

    public String getDataServerUrl() {
        return null;
    }

    public String getDataServerUserName() {
        return null;
    }

    public String getDataServerPassword() {
        return null;
    }
}
