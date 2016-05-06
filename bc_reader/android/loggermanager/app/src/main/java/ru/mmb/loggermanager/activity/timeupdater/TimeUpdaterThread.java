package ru.mmb.loggermanager.activity.timeupdater;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Set;

import ru.mmb.loggermanager.activity.MainActivity;
import ru.mmb.loggermanager.activity.settings.LoggerSettingsBluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.DevicesLoader;
import ru.mmb.loggermanager.bluetooth.ThreadMessageTypes;
import ru.mmb.loggermanager.conf.Configuration;

public class TimeUpdaterThread extends Thread {

    private static MainActivity owner = null;
    private static Handler timeUpdateHandler = null;

    public static void init(MainActivity owner, Handler timeUpdateHandler) {
        TimeUpdaterThread.owner = owner;
        TimeUpdaterThread.timeUpdateHandler = timeUpdateHandler;
    }

    private final List<DeviceInfo> pairedLoggers;

    private volatile boolean terminated = false;

    public TimeUpdaterThread() {
        super("time updater thread");
        this.pairedLoggers = DevicesLoader.loadPairedDevices();
    }

    @Override
    public void run() {
        try {
            Log.d("TIME_UPDATER", "time updater thread started");
            if (owner == null) {
                Log.d("TIME_UPDATER", "time updater thread stopped, owner == null");
            }
            autoUpdateTime();
            Log.d("TIME_UPDATER", "time updater thread finished");
        } finally {
            WakeLocker.release();
        }
    }

    private void autoUpdateTime() {
        Log.d("TIME_UPDATER", "ALARM fired");
        writeToConsole("AUTO TIME start update");
        try {
            // send time update command to all loggers
            Set<String> loggersToUpdate = Configuration.getInstance().getUpdateLoggers();
            for (DeviceInfo pairedLogger : pairedLoggers) {
                if (!loggersToUpdate.contains(pairedLogger.getDeviceName())) {
                    continue;
                }
                LoggerSettingsBluetoothClient btClient = new LoggerSettingsBluetoothClient(owner, pairedLogger, null, null);
                boolean success = btClient.updateLoggerTime();
                if (success) {
                    Log.d("TIME_UPDATER", "updated time for " + pairedLogger.getDeviceName());
                    writeToConsole("AUTO TIME updated " + pairedLogger.getDeviceName());
                } else {
                    Log.d("TIME_UPDATER", "not connected to " + pairedLogger.getDeviceName());
                    writeToConsole("AUTO TIME not connected to " + pairedLogger.getDeviceName());
                }
            }
        } catch (Exception e) {
            Log.e("TIME_UPDATER", "error", e);
            writeToConsole("AUTO TIME exception " + e.getMessage());
        }
        Log.d("TIME_UPDATER", "SENT new time to all loggers");
        writeToConsole("AUTO TIME finish update");

    }

    private void writeToConsole(String message) {
        if (timeUpdateHandler != null) {
            timeUpdateHandler.sendMessage(Message.obtain(timeUpdateHandler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }
}
