package ru.mmb.datacollector.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.bluetooth.DeviceInfo;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_DEVICE_INFO;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_SCAN_POINT;

public class ActivityStateWithScanPointAndBTDevice extends CurrentState {
    private ScanPoint currentScanPoint = null;
    private DeviceInfo currentDeviceInfo = null;
    private boolean filterJustLoggers = false;

    public ActivityStateWithScanPointAndBTDevice(String prefix) {
        super(prefix);
    }

    public ScanPoint getCurrentScanPoint() {
        return currentScanPoint;
    }

    public void setCurrentScanPoint(ScanPoint currentScanPoint) {
        this.currentScanPoint = currentScanPoint;
        fireStateChanged();
    }

    public DeviceInfo getCurrentDeviceInfo() {
        return currentDeviceInfo;
    }

    public void setCurrentDeviceInfo(DeviceInfo currentDeviceInfo) {
        this.currentDeviceInfo = currentDeviceInfo;
    }

    public boolean isFilterJustLoggers() {
        return filterJustLoggers;
    }

    public void setFilterJustLoggers(boolean filterJustLoggers) {
        this.filterJustLoggers = filterJustLoggers;
    }

    @Override
    public void save(Bundle savedInstanceState) {
        if (currentScanPoint != null)
            savedInstanceState.putSerializable(KEY_CURRENT_SCAN_POINT, currentScanPoint);
        if (currentDeviceInfo != null)
            savedInstanceState.putSerializable(KEY_CURRENT_DEVICE_INFO, currentDeviceInfo);
        savedInstanceState.putBoolean(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS, filterJustLoggers);
    }

    @Override
    public void load(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        if (savedInstanceState.containsKey(KEY_CURRENT_SCAN_POINT))
            currentScanPoint =
                    (ScanPoint) savedInstanceState.getSerializable(KEY_CURRENT_SCAN_POINT);
        if (savedInstanceState.containsKey(KEY_CURRENT_DEVICE_INFO))
            currentDeviceInfo = (DeviceInfo) savedInstanceState.getSerializable(KEY_CURRENT_DEVICE_INFO);
        if (savedInstanceState.containsKey(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS))
            filterJustLoggers = savedInstanceState.getBoolean(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS);
    }

    @Override
    protected void update(boolean fromSavedBundle) {
        if (currentScanPoint != null) {
            ScanPointsRegistry scanPoints = ScanPointsRegistry.getInstance();
            ScanPoint updatedScanPoint =
                    scanPoints.getScanPointById(currentScanPoint.getScanPointId());
            currentScanPoint = updatedScanPoint;
        }
    }

    public boolean isScanPointSelected() {
        return currentScanPoint != null;
    }

    public String getScanPointAndDeviceText(Activity activity) {
        return getSelectedScanPointString(activity) + " - " + getSelectedDeviceText(activity);
    }

    private String getSelectedScanPointString(Activity activity) {
        if (!isScanPointSelected())
            return activity.getResources().getString(R.string.input_global_no_selected_scan_point);
        else
            return currentScanPoint.getScanPointName();
    }

    private String getSelectedDeviceText(Activity activity) {
        if (!isDeviceSelected())
            return activity.getResources().getString(R.string.bluetooth_global_no_selected_device);
        else
            return currentDeviceInfo.getDeviceName();
    }

    public boolean isDeviceSelected() {
        return currentDeviceInfo != null;
    }

    @Override
    protected void loadFromExtrasBundle(Bundle extras) {
        if (extras.containsKey(KEY_CURRENT_SCAN_POINT))
            currentScanPoint = (ScanPoint) extras.getSerializable(KEY_CURRENT_SCAN_POINT);
        if (extras.containsKey(KEY_CURRENT_DEVICE_INFO))
            setCurrentDeviceInfo((DeviceInfo) extras.getSerializable(KEY_CURRENT_DEVICE_INFO));
        if (extras.containsKey(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS))
            setFilterJustLoggers(extras.getBoolean(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS));
    }

    @Override
    public void saveToSharedPreferences(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        if (getCurrentScanPoint() != null) {
            editor.putInt(getPrefix() + "." +
                          KEY_CURRENT_SCAN_POINT, getCurrentScanPoint().getScanPointId());
        }
        if (getCurrentDeviceInfo() != null) {
            editor.putString(getPrefix() + "." +
                             KEY_CURRENT_DEVICE_INFO, getCurrentDeviceInfo().saveToString());
        }
        editor.putBoolean(getPrefix() + "." +
                          KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS, isFilterJustLoggers());
        editor.commit();
    }

    @Override
    public void loadFromSharedPreferences(SharedPreferences preferences) {
        ScanPointsRegistry scanPoints = ScanPointsRegistry.getInstance();
        int scanPointId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_SCAN_POINT, -1);
        currentScanPoint = scanPoints.getScanPointById(scanPointId);
        String deviceInfoString = preferences.getString(
                getPrefix() + "." + KEY_CURRENT_DEVICE_INFO, null);
        if (deviceInfoString != null) {
            currentDeviceInfo = new DeviceInfo();
            currentDeviceInfo.loadFromString(deviceInfoString);
        } else {
            currentDeviceInfo = null;
        }
        filterJustLoggers = preferences.getBoolean(
                getPrefix() + "." + KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS, false);
    }

    @Override
    public void prepareStartActivityIntent(Intent intent, int activityRequestId) {
        switch (activityRequestId) {
            case Constants.REQUEST_CODE_DEFAULT_ACTIVITY:
            case Constants.REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY:
            case Constants.REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY:
            case Constants.REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY:
                if (getCurrentScanPoint() != null)
                    intent.putExtra(KEY_CURRENT_SCAN_POINT, getCurrentScanPoint());
                if (getCurrentDeviceInfo() != null)
                    intent.putExtra(KEY_CURRENT_DEVICE_INFO, getCurrentDeviceInfo());
        }
    }
}
