package ru.mmb.loggermanager.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.util.List;
import java.util.Set;

import ru.mmb.loggermanager.activity.settings.LoggerSettingsBluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.DevicesLoader;
import ru.mmb.loggermanager.conf.Configuration;

public class UpdateTimeAlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLocker.acquire(context);
        Log.d("TIME_UPDATER", "ALARM fired");
        try {
            List<DeviceInfo> pairedLoggers = DevicesLoader.loadPairedDevices();
            // send time update command to all loggers
            Set<String> loggersToUpdate = Configuration.getInstance().getUpdateLoggers();
            for (DeviceInfo pairedLogger : pairedLoggers) {
                if (!loggersToUpdate.contains(pairedLogger.getDeviceName())) {
                    continue;
                }
                LoggerSettingsBluetoothClient btClient = new LoggerSettingsBluetoothClient(context, pairedLogger, null, null);
                btClient.updateLoggerTime();
                Log.d("TIME_UPDATER", "updated time for " + pairedLogger.getDeviceName());
            }
        } catch (Exception e) {
            Log.e("TIME_UPDATER", "error", e);
        }
        Log.d("TIME_UPDATER", "SENT new time to all loggers");
        WakeLocker.release();
    }
}
