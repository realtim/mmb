package ru.mmb.datacollector.activity.transport.bclogger.send;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.datacollector.activity.bluetooth.BluetoothSelectDeviceActivity;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY;
import static ru.mmb.datacollector.activity.transport.bclogger.send.TransportLoggerSendActivityState.STATE_ADAPTER_DISABLED;
import static ru.mmb.datacollector.activity.transport.bclogger.send.TransportLoggerSendActivityState.STATE_DEVICE_NOT_SELECTED;
import static ru.mmb.datacollector.activity.transport.bclogger.send.TransportLoggerSendActivityState.STATE_DEVICE_SELECTED;
import static ru.mmb.datacollector.activity.transport.bclogger.send.TransportLoggerSendActivityState.STATE_SENDING;

public class TransportLoggerSendActivity extends BluetoothAdapterEnableActivity {
    private TransportLoggerSendActivityState currentState;

    private TextView labDevice;
    private Button btnSelectDevice;
    private Button btnSendData;
    private Button btnClearConsole;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private Handler bluetoothHandler;
    private TransportLoggerSendBluetoothClient bluetoothClient;
    private Thread socketThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new TransportLoggerSendActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.transport_bclogger_send);

        labDevice = (TextView) findViewById(R.id.transportBCLoggerSend_deviceLabel);
        btnSelectDevice = (Button) findViewById(R.id.transportBCLoggerSend_selectDeviceBtn);
        btnSendData = (Button) findViewById(R.id.transportBCLoggerSend_sendDataButton);
        btnClearConsole = (Button) findViewById(R.id.transportBCLoggerSend_clearConsoleButton);
        areaConsole = (TextView) findViewById(R.id.transportBCLoggerSend_consoleTextView);

        btnSelectDevice.setOnClickListener(new SelectDeviceClickListener());
        btnSendData.setOnClickListener(new SendDataClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());

        setTitle(getResources().getString(R.string.transport_bclogger_send_title));

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        bluetoothHandler = new BluetoothHandler(this, consoleAppender);

        refreshState();
    }

    private void refreshState() {
        if (currentState.getCurrentDeviceInfo() != null)
            labDevice.setText(currentState.getCurrentDeviceInfo().getDeviceName());

        switch (currentState.getState()) {
            case STATE_DEVICE_NOT_SELECTED:
                btnSelectDevice.setEnabled(true);
                btnSendData.setEnabled(false);
                btnClearConsole.setEnabled(false);
                break;
            case STATE_DEVICE_SELECTED:
                btnSelectDevice.setEnabled(true);
                btnSendData.setEnabled(true);
                btnClearConsole.setEnabled(true);
                break;
            case STATE_SENDING:
            case STATE_ADAPTER_DISABLED:
            default:
                btnSelectDevice.setEnabled(false);
                btnSendData.setEnabled(false);
                btnClearConsole.setEnabled(false);
                break;
        }
    }

    @Override
    protected void onAdapterStateChanged() {
        if (!isAdapterEnabled()) {
            currentState.setState(STATE_ADAPTER_DISABLED);
        } else if (!currentState.isDeviceSelected()) {
            currentState.setState(STATE_DEVICE_NOT_SELECTED);
        } else {
            currentState.setState(STATE_DEVICE_SELECTED);
        }
        refreshState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY:
                onSelectDeviceActivityResult(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onSelectDeviceActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            currentState.loadFromIntent(data);
        }
        if (currentState.isDeviceSelected()) {
            currentState.setState(STATE_DEVICE_SELECTED);
        } else {
            currentState.setState(STATE_DEVICE_NOT_SELECTED);
        }
        refreshState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));

        if (socketThread != null) {
            bluetoothClient.terminate();
            socketThread.interrupt();
            socketThread = null;
        }
    }

    private class SelectDeviceClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), BluetoothSelectDeviceActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY);
            intent.putExtra(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS, false);
            startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY);
        }
    }

    private class SendDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentState.setState(STATE_SENDING);
            refreshState();
            bluetoothClient = new TransportLoggerSendBluetoothClient(TransportLoggerSendActivity.this, currentState.getCurrentDeviceInfo(), bluetoothHandler);
            socketThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.sendLoggerData();
                }
            });
            socketThread.start();
        }
    }

    private class ClearConsoleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            consoleAppender.clear();
        }
    }

    private static class BluetoothHandler extends Handler {
        private final TransportLoggerSendActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private BluetoothHandler(TransportLoggerSendActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS ||
                       msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                owner.socketThread = null;
                owner.currentState.setState(STATE_DEVICE_SELECTED);
                owner.refreshState();
            }
        }
    }
}
