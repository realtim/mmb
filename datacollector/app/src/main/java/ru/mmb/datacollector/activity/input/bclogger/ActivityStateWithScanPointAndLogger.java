package ru.mmb.datacollector.activity.input.bclogger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.Constants;
import ru.mmb.datacollector.activity.CurrentState;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.bclogger.LoggerInfo;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_LOGGER_INFO;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_SCAN_POINT;

public class ActivityStateWithScanPointAndLogger extends CurrentState {
    private ScanPoint currentScanPoint = null;
    private LoggerInfo currentLoggerInfo = null;

    public ActivityStateWithScanPointAndLogger(String prefix) {
        super(prefix);
    }

    public ScanPoint getCurrentScanPoint() {
        return currentScanPoint;
    }

    public void setCurrentScanPoint(ScanPoint currentScanPoint) {
        this.currentScanPoint = currentScanPoint;
        fireStateChanged();
    }

    public LoggerInfo getCurrentLoggerInfo() {
        return currentLoggerInfo;
    }

    public void setCurrentLoggerInfo(LoggerInfo currentLoggerInfo) {
        this.currentLoggerInfo = currentLoggerInfo;
    }

    @Override
    public void save(Bundle savedInstanceState) {
        if (currentScanPoint != null)
            savedInstanceState.putSerializable(KEY_CURRENT_SCAN_POINT, currentScanPoint);
        if (currentLoggerInfo != null)
            savedInstanceState.putSerializable(KEY_CURRENT_LOGGER_INFO, currentLoggerInfo);
    }

    @Override
    public void load(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        if (savedInstanceState.containsKey(KEY_CURRENT_SCAN_POINT))
            currentScanPoint =
                    (ScanPoint) savedInstanceState.getSerializable(KEY_CURRENT_SCAN_POINT);
        if (savedInstanceState.containsKey(KEY_CURRENT_LOGGER_INFO))
            currentLoggerInfo = (LoggerInfo) savedInstanceState.getSerializable(KEY_CURRENT_LOGGER_INFO);
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

    public String getScanPointAndLoggerText(Activity activity) {
        return getSelectedScanPointString(activity) + " - " + getSelectedLoggerText(activity);
    }

    private String getSelectedScanPointString(Activity activity) {
        if (!isScanPointSelected())
            return activity.getResources().getString(R.string.input_global_no_selected_scan_point);
        else
            return currentScanPoint.getScanPointName();
    }

    private String getSelectedLoggerText(Activity activity) {
        if (!isLoggerSelected())
            return activity.getResources().getString(R.string.input_global_no_selected_logger);
        else
            return currentLoggerInfo.getLoggerName();
    }

    @Override
    protected void loadFromExtrasBundle(Bundle extras) {
        if (extras.containsKey(KEY_CURRENT_SCAN_POINT))
            currentScanPoint = (ScanPoint) extras.getSerializable(KEY_CURRENT_SCAN_POINT);
        if (extras.containsKey(KEY_CURRENT_LOGGER_INFO))
            setCurrentLoggerInfo((LoggerInfo) extras.getSerializable(KEY_CURRENT_LOGGER_INFO));
    }

    @Override
    public void saveToSharedPreferences(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        if (getCurrentScanPoint() != null) {
            editor.putInt(getPrefix() + "." +
                          KEY_CURRENT_SCAN_POINT, getCurrentScanPoint().getScanPointId());
        }
        if (getCurrentLoggerInfo() != null) {
            editor.putString(getPrefix() + "." +
                             KEY_CURRENT_LOGGER_INFO, getCurrentLoggerInfo().saveToString());
        }
        editor.commit();
    }

    @Override
    public void loadFromSharedPreferences(SharedPreferences preferences) {
        ScanPointsRegistry scanPoints = ScanPointsRegistry.getInstance();
        int scanPointId = preferences.getInt(getPrefix() + "." + KEY_CURRENT_SCAN_POINT, -1);
        currentScanPoint = scanPoints.getScanPointById(scanPointId);
        String loggerInfoString = preferences.getString(
                getPrefix() + "." + KEY_CURRENT_LOGGER_INFO, null);
        if (loggerInfoString != null) {
            currentLoggerInfo = new LoggerInfo();
            currentLoggerInfo.loadFromString(loggerInfoString);
        } else {
            currentLoggerInfo = null;
        }
    }

    @Override
    public void prepareStartActivityIntent(Intent intent, int activityRequestId) {
        switch (activityRequestId) {
            case Constants.REQUEST_CODE_DEFAULT_ACTIVITY:
            case Constants.REQUEST_CODE_INPUT_BCLOGGER_SELECT_ACTIVITY:
            case Constants.REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY:
            case Constants.REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY:
                if (getCurrentScanPoint() != null)
                    intent.putExtra(KEY_CURRENT_SCAN_POINT, getCurrentScanPoint());
                if (getCurrentLoggerInfo() != null)
                    intent.putExtra(KEY_CURRENT_LOGGER_INFO, getCurrentLoggerInfo());
        }
    }

    public boolean isLoggerSelected() {
        return currentLoggerInfo != null;
    }
}
