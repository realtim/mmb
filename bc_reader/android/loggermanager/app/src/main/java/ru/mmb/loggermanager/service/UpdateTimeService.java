package ru.mmb.loggermanager.service;

import android.content.Intent;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.List;
import java.util.Set;

import ru.mmb.loggermanager.activity.settings.LoggerSettingsBluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.DevicesLoader;
import ru.mmb.loggermanager.conf.Configuration;

public class UpdateTimeService extends WakefulIntentService {

    private final List<DeviceInfo> pairedLoggers;

    public UpdateTimeService() {
        super("update time service");
        this.pairedLoggers = DevicesLoader.loadPairedDevices();
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        // send time update command to all loggers
        Set<String> loggersToUpdate = Configuration.getInstance().getUpdateLoggers();
        for (DeviceInfo pairedLogger : pairedLoggers) {
            if (!loggersToUpdate.contains(pairedLogger.getDeviceName())) {
                continue;
            }
            LoggerSettingsBluetoothClient btClient = new LoggerSettingsBluetoothClient(this, pairedLogger, null, null);
            btClient.updateLoggerTime();
        }
        Log.d("TIME_UPDATER", "SENT new time to all loggers");
    }
}
