package ru.mmb.loggermanager.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.loggermanager.bluetooth.BluetoothClient;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.ThreadMessageTypes;
import ru.mmb.loggermanager.widget.ConsoleMessagesAppender;

public class MainActivity extends BluetoothAdapterEnableActivity {

    private LinearLayout globalContainerPanel;

    private DeviceInfo selectedLogger = null;

    private ToggleButton panelsToggle;
    private ViewFlipper panelsFlipper;

    private LoggerSettings loggerSettings = new LoggerSettings();
    private SettingsPanel settingsPanel;

    private ConsoleMessagesAppender consoleAppender;
    private BluetoothClient bluetoothClient;
    private Thread runningThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        globalContainerPanel = (LinearLayout) findViewById(R.id.main_globalContainerPanel);
        panelsToggle = (ToggleButton) findViewById(R.id.main_switchPanelsToggle);
        panelsToggle.setOnClickListener(new PanelsSwitchListener());
        panelsFlipper = (ViewFlipper) findViewById(R.id.main_panelsFlipper);

        new SelectLoggerPanel(this);
        settingsPanel = new SettingsPanel(this);

        TextView consoleTextView = (TextView) findViewById(R.id.main_consoleTextView);
        consoleAppender = new ConsoleMessagesAppender(consoleTextView);
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

    private void setControlsEnabled(boolean value) {
        panelsToggle.setEnabled(value);
        panelsFlipper.setEnabled(value);
        settingsPanel.setControlsEnabled(value);
    }

    public void selectedLoggerChanged(DeviceInfo selectedLogger) {
        this.selectedLogger = selectedLogger;
        refreshState();
    }

    @Override
    protected void onAdapterStateChanged() {
        refreshState();
    }

    private void refreshState() {
        if (!isAdapterEnabled()) {
            globalContainerPanel.setEnabled(false);
            return;
        } else if (!globalContainerPanel.isEnabled()) {
            globalContainerPanel.setEnabled(true);
        }

        setControlsEnabled(false);
        settingsPanel.clearControls();
        if (selectedLogger != null) {
            reloadSelectedLoggerSettings();
        }
    }

    public void reloadSelectedLoggerSettings() {
        runLoggerSettingsBtClientMethod(new SettingsRunnable() {
            @Override
            public void run() {
                settingsBtClient.reloadSettings();
            }
        }, new ReloadSettingsBtHandler());
    }

    public void runLoggerSettingsBtClientMethod(SettingsRunnable runnable, Handler handler) {
        LoggerSettingsBluetoothClient settingsBtClient =
                new LoggerSettingsBluetoothClient(this, selectedLogger, handler, loggerSettings);
        bluetoothClient = settingsBtClient;
        setControlsEnabled(false);
        runnable.setSettingsBtClient(settingsBtClient);
        runningThread = new Thread(runnable);
        runningThread.start();
    }

    public void sendLoggerSettingsCommand(final String command) {
        runLoggerSettingsBtClientMethod(new SettingsRunnable() {
            @Override
            public void run() {
                settingsBtClient.sendCommand(command);
            }
        }, new SendSettingsBtHandler());
    }

    public void updateLoggerTime() {
        runLoggerSettingsBtClientMethod(new SettingsRunnable() {
            @Override
            public void run() {
                settingsBtClient.updateLoggerTime();
            }
        }, new SendSettingsBtHandler());
    }

    private class PanelsSwitchListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (panelsToggle.isChecked()) {
                panelsFlipper.showNext();
            } else {
                panelsFlipper.showPrevious();
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

    private abstract class SettingsRunnable implements Runnable {
        protected LoggerSettingsBluetoothClient settingsBtClient;

        public void setSettingsBtClient(LoggerSettingsBluetoothClient settingsBtClient) {
            this.settingsBtClient = settingsBtClient;
        }
    }
}
