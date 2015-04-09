package ru.mmb.datacollector.activity.input.bclogger.settings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;

public class LoggerSettingsActivity extends Activity {
    private ScanPointsRegistry scanPoints;

    private LoggerSettingsActivityState currentState;

    private EditText editLoggerId;
    private Spinner comboScanpoint;
    private TextView labSelectedScanpoint;
    private EditText editPattern;
    private TextView labLoggerTime;
    private CheckBox checkLength;
    private CheckBox checkDigits;
    private Button btnReload;
    private Button btnSend;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private LoggerSettingsBluetoothClient bluetoothClient;
    private Thread runningThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        scanPoints = ScanPointsRegistry.getInstance();

        currentState = new LoggerSettingsActivityState("input.bclogger.settings");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_settings);

        editLoggerId = (EditText) findViewById(R.id.inputBCLoggerSettings_loggerIdEdit);
        comboScanpoint = (Spinner) findViewById(R.id.inputBCLoggerSettings_scanpointSpinner);
        labSelectedScanpoint = (TextView) findViewById(R.id.inputBCLoggerSettings_selectedScanpointLabel);
        editPattern = (EditText) findViewById(R.id.inputBCLoggerSettings_patternEdit);
        labLoggerTime = (TextView) findViewById(R.id.inputBCLoggerSettings_loggerTimeLabel);
        checkLength = (CheckBox) findViewById(R.id.inputBCLoggerSettings_lengthCheck);
        checkDigits = (CheckBox) findViewById(R.id.inputBCLoggerSettings_digitsCheck);
        btnReload = (Button) findViewById(R.id.inputBCLoggerSettings_reloadButton);
        btnSend = (Button) findViewById(R.id.inputBCLoggerSettings_sendButton);
        areaConsole = (TextView) findViewById(R.id.inputBCLoggerSettings_consoleTextView);

        setTitle(currentState.getScanPointAndDeviceText(this));
        setComboScanPointAdapter();
        labSelectedScanpoint.setText(currentState.getCurrentScanPoint().getScanPointName());
        btnReload.setOnClickListener(new ReloadClickListener());
        btnSend.setOnClickListener(new SendClickListener());

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        Handler bluetoothMessagesHandler = new BluetoothHandler(this, consoleAppender);
        bluetoothClient = new LoggerSettingsBluetoothClient(this, currentState.getCurrentDeviceInfo(), bluetoothMessagesHandler, currentState);

        setControlsEnabled(false);
        btnReload.setEnabled(true);
    }

    private void setComboScanPointAdapter() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, scanPoints.getScanPointNamesArray());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        comboScanpoint.setAdapter(adapter);
    }

    private void updateControlsAfterCommunication() {
        boolean enabled = currentState.isCommunicationSuccess();
        setControlsEnabled(enabled);
        if (!enabled) {
            // Activate Reload button in any case.
            btnReload.setEnabled(true);
        }
        refreshState();
    }

    private void setControlsEnabled(boolean value) {
        comboScanpoint.setEnabled(value);
        editPattern.setEnabled(value);
        checkLength.setEnabled(value);
        checkDigits.setEnabled(value);
        btnSend.setEnabled(value);
        btnReload.setEnabled(value);
    }

    private void refreshState() {
        if (currentState.getLoggerId() == null) {
            editLoggerId.setText("");
        } else {
            editLoggerId.setText(currentState.getLoggerId());
        }
        if (currentState.getScanpointOrder() == null) {
            comboScanpoint.setSelection(0);
        } else {
            int scanpointOrder = Integer.parseInt(currentState.getScanpointOrder());
            int scanpointPos = scanPoints.getScanPointIndex(scanPoints.getScanPointByOrder(scanpointOrder));
            comboScanpoint.setSelection(scanpointPos);
        }
        if (currentState.getPattern() == null) {
            editPattern.setText("");
        } else {
            editPattern.setText(currentState.getPattern());
        }
        checkLength.setChecked(currentState.isLengthCheck());
        checkDigits.setChecked(currentState.isDigitsOnly());
        if (currentState.getLoggerTime() == null) {
            labLoggerTime.setText("");
        } else {
            labLoggerTime.setText(currentState.getLoggerTime());
        }
    }

    private void updateCurrentStateFromControls() {
        // scanpointOrder
        ScanPoint scanpoint = scanPoints.getScanPointByIndex(comboScanpoint.getSelectedItemPosition());
        String scanpointOrder = String.format("%02d", scanpoint.getScanPointOrder());
        currentState.setScanpointOrder(scanpointOrder);
        // pattern
        currentState.setPattern(editPattern.getText().toString());
        // lengthCheck
        currentState.setLengthCheck(checkLength.isChecked());
        // digitsOnly
        currentState.setDigitsOnly(checkDigits.isChecked());
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

    private class ReloadClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(false);
            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.reloadSettings();
                }
            });
            runningThread.start();
        }
    }

    private class SendClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            updateCurrentStateFromControls();
            setControlsEnabled(false);
            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.sendSettings();
                }
            });
            runningThread.start();
        }
    }

    private static class BluetoothHandler extends Handler {
        private final LoggerSettingsActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private BluetoothHandler(LoggerSettingsActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                owner.runningThread = null;
                owner.updateControlsAfterCommunication();
            }
        }
    }
}
