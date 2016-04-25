package ru.mmb.loggermanager.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.filedialog.FileDialog;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.activity.dataload.LoggerDataLoadBluetoothClient;
import ru.mmb.loggermanager.activity.settings.LoggerSettings;
import ru.mmb.loggermanager.activity.settings.LoggerSettingsBluetoothClient;
import ru.mmb.loggermanager.activity.timeupdater.TimeUpdaterThread;
import ru.mmb.loggermanager.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.loggermanager.bluetooth.BluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.ThreadMessageTypes;
import ru.mmb.loggermanager.conf.Configuration;
import ru.mmb.loggermanager.widget.ConsoleMessagesAppender;

import static ru.mmb.loggermanager.activity.Constants.REQUEST_CODE_SAVE_DIR_DIALOG;

public class MainActivity extends BluetoothAdapterEnableActivity {

    private boolean adapterActive = false;
    private DeviceInfo selectedLogger = null;

    private RadioButton settingsRadio;
    private RadioButton logsRadio;
    private RadioButton autoTimeRadio;
    private ViewFlipper panelsFlipper;
    private int currentRadioTag = 0;

    private LoggerSettings loggerSettings = new LoggerSettings();
    private SettingsPanel settingsPanel;
    private LogsPanel logsPanel;

    private ConsoleMessagesAppender consoleAppender;
    private BluetoothClient bluetoothClient;
    private Thread runningThread = null;

    private TimeUpdaterThread timeUpdaterThread = null;
    private ProgressBar timeUpdaterProgress = null;
    private CheckBox autoUpdateTimeCheck;

    public ProgressBar getTimeUpdaterProgress() {
        return timeUpdaterProgress;
    }

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

        new SelectLoggerPanel(this);
        settingsPanel = new SettingsPanel(this);
        logsPanel = new LogsPanel(this);

        TextView consoleTextView = (TextView) findViewById(R.id.main_consoleTextView);
        consoleAppender = new ConsoleMessagesAppender(consoleTextView);

        timeUpdaterProgress = (ProgressBar) findViewById(R.id.main_timeUpdaterProgress);

        autoUpdateTimeCheck = (CheckBox) findViewById(R.id.main_autoUpdateTimeCheckBox);
        autoUpdateTimeCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (autoUpdateTimeCheck.isChecked()) {
                    startTimeUpdaterThread();
                } else if (timeUpdaterThread != null) {
                    stopTimeUpdaterThread();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (runningThread != null) {
            bluetoothClient.terminate();
            runningThread.interrupt();
            runningThread = null;
        }

        if (timeUpdaterThread != null) {
            stopTimeUpdaterThread();
        }
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

    private void startTimeUpdaterThread() {
        if (timeUpdaterThread != null) {
            stopTimeUpdaterThread();
        }
        if (isAdapterEnabled()) {
            timeUpdaterThread = new TimeUpdaterThread(this, new TimeUpdateHandler());
            timeUpdaterThread.start();
        }
    }

    private void stopTimeUpdaterThread() {
        timeUpdaterThread.terminate();
        timeUpdaterThread.interrupt();
    }

    private void refreshState() {
        setControlsEnabled(false);
        settingsPanel.clearControls();
        if (isAdapterEnabled() && selectedLogger != null) {
            reloadSelectedLoggerSettings();
        }
    }

    public void runLoggerSettingsBtClientMethod(BluetoothClientRunnable<LoggerSettingsBluetoothClient> runnable, Handler handler) {
        setControlsEnabled(false);
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

    private class RadioCheckListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                int checkedRadioTag = 0;
                if (buttonView == logsRadio) {
                    checkedRadioTag = 1;
                } else if (buttonView == autoTimeRadio) {
                    checkedRadioTag = 2;
                }
                if (checkedRadioTag == currentRadioTag) {
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
                reloadSelectedLoggerSettings();
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                runningThread = null;
                settingsPanel.clearControls();
                setControlsEnabled(false);
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
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                runningThread = null;
                settingsPanel.clearControls();
                setControlsEnabled(false);
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
            }
        }
    }

    private abstract class BluetoothClientRunnable<T> implements Runnable {
        protected T bluetoothClient;

        public void setBluetoothClient(T bluetoothClient) {
            this.bluetoothClient = bluetoothClient;
        }
    }

    private class TimeUpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                Log.d("TIME_UPDATER", (String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                Log.d("TIME_UPDATER", "time update SUCCESS");
            } else {
                Log.d("TIME_UPDATER", "time update ERROR");
            }
        }
    }
}
