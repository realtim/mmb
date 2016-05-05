package ru.mmb.loggermanager.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.filedialog.FileDialog;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.activity.dataload.LoggerDataLoadBluetoothClient;
import ru.mmb.loggermanager.activity.settings.LoggerSettings;
import ru.mmb.loggermanager.activity.settings.LoggerSettingsBluetoothClient;
import ru.mmb.loggermanager.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.loggermanager.bluetooth.BluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.ThreadMessageTypes;
import ru.mmb.loggermanager.conf.Configuration;
import ru.mmb.loggermanager.service.UpdateTimeAlarmReceiver;
import ru.mmb.loggermanager.service.WakeLocker;
import ru.mmb.loggermanager.widget.ConsoleMessagesAppender;

import static ru.mmb.loggermanager.activity.Constants.REQUEST_CODE_SAVE_DIR_DIALOG;

public class MainActivity extends BluetoothAdapterEnableActivity {

    private boolean adapterActive = false;
    private DeviceInfo selectedLogger = null;

    private RadioButton settingsRadio;
    private RadioButton logsRadio;
    private RadioButton autoTimeRadio;
    private RadioButton consoleRadio;
    private ViewFlipper panelsFlipper;
    private int currentRadioTag = 0;

    private LoggerSettings loggerSettings = new LoggerSettings();
    private SettingsPanel settingsPanel;
    private LogsPanel logsPanel;
    private AutoUpdatePanel autoUpdatePanel;

    private TextView btStatusLabel = null;

    private ConsoleMessagesAppender consoleAppender;
    private BluetoothClient bluetoothClient;
    private Thread runningThread = null;

    private AlarmManager alarmManager = null;
    private PendingIntent pendingIntent = null;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Configuration.getInstance().loadConfiguration(this);

        panelsFlipper = (ViewFlipper) findViewById(R.id.main_panelsFlipper);

        RadioCheckListener radioCheckListener = new RadioCheckListener();
        settingsRadio = (RadioButton) findViewById(R.id.main_settingsRadioButton);
        settingsRadio.setOnCheckedChangeListener(radioCheckListener);
        logsRadio = (RadioButton) findViewById(R.id.main_logsRadioButton);
        logsRadio.setOnCheckedChangeListener(radioCheckListener);
        autoTimeRadio = (RadioButton) findViewById(R.id.main_autoTimeRadioButton);
        autoTimeRadio.setOnCheckedChangeListener(radioCheckListener);
        consoleRadio = (RadioButton) findViewById(R.id.main_consoleRadioButton);
        if (consoleRadio != null) {
            consoleRadio.setOnCheckedChangeListener(radioCheckListener);
        }

        new SelectLoggerPanel(this);
        settingsPanel = new SettingsPanel(this);
        logsPanel = new LogsPanel(this);
        autoUpdatePanel = new AutoUpdatePanel(this);

        TextView consoleTextView = (TextView) findViewById(R.id.main_consoleTextView);
        consoleAppender = new ConsoleMessagesAppender(consoleTextView);

        btStatusLabel = (TextView) findViewById(R.id.main_btStatusTextView);
        updateBtStatusLabel("");

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent updateTimeIntent = new Intent(this, UpdateTimeAlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, updateTimeIntent, 0);
        WakeLocker.init(this);
        UpdateTimeAlarmReceiver.init(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (runningThread != null) {
            bluetoothClient.terminate();
            runningThread.interrupt();
            runningThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopTimeUpdaterAlarms();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SAVE_DIR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    logsPanel.changeSaveDir(data.getStringExtra(FileDialog.RESULT_PATH));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setControlsEnabled(boolean value) {
        settingsPanel.setControlsEnabled(value);
        logsPanel.setControlsEnabled(value);
    }

    public void selectedLoggerChanged(DeviceInfo selectedLogger) {
        this.selectedLogger = selectedLogger;
        refreshState();
    }

    @Override
    protected void onAdapterStateChanged() {
        if (adapterActive != isAdapterEnabled()) {
            consoleAppender.appendMessage("changing adapter state");
            adapterActive = isAdapterEnabled();
            refreshState();
        }
    }

    public void startTimeUpdaterAlarms() {
        stopTimeUpdaterAlarms();
        if (isAdapterEnabled() && alarmManager != null) {
            int alarmInterval = Configuration.getInstance().getUpdatePeriodMinutes();
            // FIXME restore pauseDuration to minutes (60000)
            long pauseDuration = 20000L;
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + pauseDuration,
                    alarmInterval * pauseDuration, pendingIntent);
            Log.d("TIME_UPDATER", "alarm scheduled");
        }
    }

    public void stopTimeUpdaterAlarms() {
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d("TIME_UPDATER", "alarm cancelled");
        }
    }

    private void refreshState() {
        setControlsEnabled(false);
        autoUpdatePanel.setControlsEnabled(false);
        settingsPanel.clearControls();
        if (isAdapterEnabled()) {
            autoUpdatePanel.setControlsEnabled(true);
            if (selectedLogger != null) {
                reloadSelectedLoggerSettings();
            }
        } else {
            writeToConsole("TURN ON BT ADAPTER!");
            updateBtStatusLabel("TURN ON BT ADAPTER!");
        }
    }

    public void runLoggerSettingsBtClientMethod(BluetoothClientRunnable<LoggerSettingsBluetoothClient> runnable, Handler handler) {
        setControlsEnabled(false);
        updateBtStatusLabel("bluetooth STARTED");
        LoggerSettingsBluetoothClient settingsBtClient =
                new LoggerSettingsBluetoothClient(this, selectedLogger, handler, loggerSettings);
        bluetoothClient = settingsBtClient;
        runnable.setBluetoothClient(settingsBtClient);
        runningThread = new Thread(runnable);
        runningThread.start();
    }

    public void reloadSelectedLoggerSettings() {
        runLoggerSettingsBtClientMethod(new BluetoothClientRunnable<LoggerSettingsBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.reloadSettings();
            }
        }, new ReloadSettingsBtHandler());
    }

    public void sendLoggerSettingsCommand(final String command) {
        runLoggerSettingsBtClientMethod(new BluetoothClientRunnable<LoggerSettingsBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.sendCommand(command);
            }
        }, new SendSettingsBtHandler());
    }

    public void updateLoggerTime() {
        runLoggerSettingsBtClientMethod(new BluetoothClientRunnable<LoggerSettingsBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.updateLoggerTime();
            }
        }, new SendSettingsBtHandler());
    }

    public void runLoggerDataLoadBtClientMethod(BluetoothClientRunnable<LoggerDataLoadBluetoothClient> runnable, Handler handler) {
        setControlsEnabled(false);
        updateBtStatusLabel("bluetooth STARTED");
        LoggerDataLoadBluetoothClient dataLoadBtClient =
                new LoggerDataLoadBluetoothClient(this, selectedLogger, handler);
        bluetoothClient = dataLoadBtClient;
        runnable.setBluetoothClient(dataLoadBtClient);
        runningThread = new Thread(runnable);
        runningThread.start();
    }

    public void loadLogsFromLogger() {
        runLoggerDataLoadBtClientMethod(new BluetoothClientRunnable<LoggerDataLoadBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.loadLogData();
            }
        }, new LoadDataBtHandler());
    }

    public void loadDebugFromLogger() {
        runLoggerDataLoadBtClientMethod(new BluetoothClientRunnable<LoggerDataLoadBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.loadErrorsData();
            }
        }, new LoadDataBtHandler());
    }

    public void sendLogsCommand(final String command) {
        runLoggerDataLoadBtClientMethod(new BluetoothClientRunnable<LoggerDataLoadBluetoothClient>() {
            @Override
            public void run() {
                bluetoothClient.sendCommand(command);
            }
        }, new LoadDataBtHandler());
    }

    public void writeToConsole(String message) {
        consoleAppender.appendMessage(message);
    }

    private void updateBtStatusLabel(String message) {
        if (btStatusLabel != null) {
            btStatusLabel.setText(message);
        }
    }

    private class RadioCheckListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                int checkedRadioTag = -1;
                if (buttonView == settingsRadio) {
                    checkedRadioTag = 0;
                } else if (buttonView == logsRadio) {
                    checkedRadioTag = 1;
                } else if (buttonView == autoTimeRadio) {
                    checkedRadioTag = 2;
                } else if (buttonView == consoleRadio) {
                    checkedRadioTag = 3;
                }
                if (checkedRadioTag == -1 || checkedRadioTag == currentRadioTag) {
                    return;
                }
                int diff = checkedRadioTag - currentRadioTag;
                int directionSign = (int) Math.signum(diff);
                diff = Math.abs(diff);
                if (directionSign < 0) {
                    for (int i = 0; i < diff; i++) {
                        panelsFlipper.showPrevious();
                    }
                } else {
                    for (int i = 0; i < diff; i++) {
                        panelsFlipper.showNext();
                    }
                }
                currentRadioTag = checkedRadioTag;
            }
        }
    }

    private class SendSettingsBtHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                runningThread = null;
                setControlsEnabled(true);
                updateBtStatusLabel("bluetooth OK");
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                runningThread = null;
                settingsPanel.clearControls();
                setControlsEnabled(false);
                updateBtStatusLabel("bluetooth ERROR");
            }
        }
    }

    private class ReloadSettingsBtHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                runningThread = null;
                settingsPanel.updateLoggerSettings(loggerSettings);
                setControlsEnabled(true);
                updateBtStatusLabel("bluetooth OK");
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                runningThread = null;
                settingsPanel.clearControls();
                setControlsEnabled(false);
                updateBtStatusLabel("bluetooth ERROR");
            }
        }
    }

    private class LoadDataBtHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else {
                // no matter was error or not
                runningThread = null;
                setControlsEnabled(true);
                updateBtStatusLabel("bluetooth DATALOAD FINISHED");
            }
        }
    }

    private abstract class BluetoothClientRunnable<T> implements Runnable {
        protected T bluetoothClient;

        public void setBluetoothClient(T bluetoothClient) {
            this.bluetoothClient = bluetoothClient;
        }
    }
}
