package ru.mmb.datacollector.activity.transport.bclogger.receive;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

import static ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveActivityState.STATE_ADAPTER_DISABLED;
import static ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveActivityState.STATE_IDLE;
import static ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveActivityState.STATE_LISTENING;
import static ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveActivityState.STATE_RECEIVING;

public class TransportLoggerReceiveActivity extends BluetoothAdapterEnableActivity {
    private TransportLoggerReceiveActivityState currentState;

    private Button btnStartListening;
    private Button btnStopListening;
    private Button btnClearConsole;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private Handler bluetoothHandler;
    private TransportLoggerReceiveBluetoothServer bluetoothServer;
    private TransportLoggerReceiveBluetoothClient bluetoothClient;
    private Thread acceptThread = null;
    private Thread socketThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new TransportLoggerReceiveActivityState();
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.transport_bclogger_receive);

        btnStartListening = (Button) findViewById(R.id.transportBCLoggerReceive_startListeningButton);
        btnStopListening = (Button) findViewById(R.id.transportBCLoggerReceive_stopListeningButton);
        btnClearConsole = (Button) findViewById(R.id.transportBCLoggerReceive_clearConsoleButton);
        areaConsole = (TextView) findViewById(R.id.transportBCLoggerReceive_consoleTextView);

        btnStartListening.setOnClickListener(new StartListeningClickListener());
        btnStopListening.setOnClickListener(new StopListeningClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        bluetoothHandler = new BluetoothHandler(this, consoleAppender);

        setTitle(getResources().getString(R.string.transport_bclogger_receive_title));

        refreshState();
    }

    private void refreshState() {
        switch (currentState.getState()) {
            case STATE_IDLE:
                btnStartListening.setEnabled(true);
                btnStopListening.setEnabled(false);
                btnClearConsole.setEnabled(true);
                break;
            case STATE_LISTENING:
                btnStartListening.setEnabled(false);
                btnStopListening.setEnabled(true);
                btnClearConsole.setEnabled(false);
                break;
            case STATE_RECEIVING:
            case STATE_ADAPTER_DISABLED:
            default:
                btnStartListening.setEnabled(false);
                btnStopListening.setEnabled(false);
                btnClearConsole.setEnabled(false);
                break;
        }
    }

    @Override
    protected void onAdapterStateChanged() {
        if (isAdapterEnabled()) {
            currentState.setState(STATE_IDLE);
        } else {
            currentState.setState(STATE_ADAPTER_DISABLED);
        }
        refreshState();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (acceptThread != null) {
            bluetoothServer.terminate();
            acceptThread = null;
        }

        if (socketThread != null) {
            bluetoothClient.terminate();
            socketThread.interrupt();
            socketThread = null;
        }
    }

    private void startReceiving(BluetoothSocket socket) {
        currentState.setState(STATE_RECEIVING);
        refreshState();
        bluetoothClient = new TransportLoggerReceiveBluetoothClient(this, bluetoothHandler, socket);
        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothClient.receive();
            }
        });
        socketThread.start();
    }

    private class StartListeningClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            currentState.setState(STATE_LISTENING);
            refreshState();
            bluetoothServer = new TransportLoggerReceiveBluetoothServer(bluetoothHandler);
            acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothServer.accept();
                }
            });
            acceptThread.start();
        }
    }

    private class StopListeningClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            bluetoothServer.terminate();
            acceptThread = null;
            currentState.setState(STATE_IDLE);
            refreshState();
        }
    }

    private class ClearConsoleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            consoleAppender.clear();
        }
    }

    private static class BluetoothHandler extends Handler {
        private final TransportLoggerReceiveActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private BluetoothHandler(TransportLoggerReceiveActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_ACCEPT_ERROR) {
                owner.acceptThread = null;
                owner.currentState.setState(STATE_IDLE);
                owner.refreshState();
            } else if (msg.what == ThreadMessageTypes.MSG_ACCEPT_SUCCESS) {
                owner.acceptThread = null;
                owner.startReceiving((BluetoothSocket) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS ||
                       msg.what == ThreadMessageTypes.MSG_FINISHED_ERROR) {
                owner.socketThread = null;
                owner.currentState.setState(STATE_IDLE);
                owner.refreshState();
            }
        }
    }
}
