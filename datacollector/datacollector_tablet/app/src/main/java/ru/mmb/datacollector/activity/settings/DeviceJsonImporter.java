package ru.mmb.datacollector.activity.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.util.JSONUtils;

public class DeviceJsonImporter {
    private static final String FIELD_DEVICES = "Devices";

    private static final String FIELD_DEVICE_ID = "device_id";
    private static final String FIELD_DEVICE_NAME = "device_name";
    private static final String FIELD_USER_ID = "user_id";
    private static final String FIELD_USER_PASSWORD = "user_password";

    private final SettingsActivity settingsActivity;
    private JSONArray deviceRecords;

    public DeviceJsonImporter(SettingsActivity settingsActivity) {
        this.settingsActivity = settingsActivity;
    }

    public boolean prepareJsonObjects(String deviceJsonName) {
        try {
            String jsonString = JSONUtils.readFromFile(deviceJsonName, 8192);
            JSONObject packageObject = new JSONObject(jsonString);
            deviceRecords = packageObject.getJSONArray(FIELD_DEVICES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void showImportDialog() {
        try {
            if (deviceRecords.length() >= 1) {
                CharSequence[] deviceNames = getDeviceNames();
                AlertDialog.Builder builder = new AlertDialog.Builder(settingsActivity);
                builder.setTitle(R.string.settings_json_dialog_title);
                builder.setItems(deviceNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importDeviceRecord(which);
                        settingsActivity.refreshState();
                    }
                });
                builder.show();
            }
        } catch (Exception e) {
            // do nothing
            Log.e("DEVICE_JSON", "dialog creation error", e);
        }
    }

    private CharSequence[] getDeviceNames() throws JSONException {
        CharSequence[] result = new CharSequence[deviceRecords.length()];
        for (int i = 0; i < deviceRecords.length(); i++) {
            JSONObject deviceObject = deviceRecords.getJSONObject(i);
            String deviceName = deviceObject.getString(FIELD_DEVICE_NAME);
            result[i] = deviceName;
        }
        return result;
    }

    public void importDeviceRecord(int deviceRecordIndex) {
        try {
            JSONObject deviceObject = deviceRecords.getJSONObject(deviceRecordIndex);
            Settings.getInstance().setDeviceId(deviceObject.getString(FIELD_DEVICE_ID));
            Settings.getInstance().setUserId(deviceObject.getString(FIELD_USER_ID));
            Settings.getInstance().setTranspUserId(deviceObject.getString(FIELD_USER_ID));
            Settings.getInstance().setTranspUserPassword(deviceObject.getString(FIELD_USER_PASSWORD));
        } catch (Exception e) {
            // do nothing
        }
    }
}
