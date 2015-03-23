package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.bclogger.ActivityStateWithScanPointAndLogger;
import ru.mmb.datacollector.activity.input.bclogger.ConsoleMessagesAppender;
import ru.mmb.datacollector.activity.input.bclogger.ThreadMessageTypes;
import ru.mmb.datacollector.model.registry.Settings;

public class LoggerDataLoadActivity extends Activity {
    ActivityStateWithScanPointAndLogger currentState;

    private Button btnGetLog;
    private Button btnGetDebug;
    // private Button btnClearDevice;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private LoggerDataLoadBluetoothClient bluetoothClient;
    private Thread runningThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithScanPointAndLogger("input.bclogger.dataload");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_dataload);

        btnGetLog = (Button) findViewById(R.id.inputBCLoggerDataload_getLogButton);
        btnGetDebug = (Button) findViewById(R.id.inputBCLoggerDataload_getDebugButton);
        // btnClearDevice = (Button) findViewById(R.id.inputBCLoggerDataload_clearDeviceButton);
        areaConsole = (TextView) findViewById(R.id.inputBCLoggerDataload_consoleTextView);

        setTitle(currentState.getScanPointAndLoggerText(this));

        btnGetLog.setOnClickListener(new GetLogClickListener());
        // btnClearDevice.setOnClickListener(new ClearDeviceClickListener());

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        Handler bluetoothMessagesHandler = new BluetoothHandler(this, consoleAppender);
        bluetoothClient = new LoggerDataLoadBluetoothClient(this, currentState.getCurrentLoggerInfo(), bluetoothMessagesHandler, currentState.getCurrentScanPoint());

        setControlsEnabled(true);
    }

    private void setControlsEnabled(boolean value) {
        btnGetLog.setEnabled(value);
        btnGetDebug.setEnabled(value);
        // btnClearDevice.setEnabled(value);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (runningThread != null) {
            bluetoothClient.terminate();
            runningThread.interrupt();
        }
    }

    private class GetLogClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(false);
            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.loadLogData();
                }
            });
            runningThread.start();
        }
    }

    /*
    private class ClearDeviceClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(false);
            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.clearDevice();
                }
            });
            runningThread.start();
        }
    }
    */

    private static class BluetoothHandler extends Handler {
        private final LoggerDataLoadActivity owner;
        private final ConsoleMessagesAppender consoleAppender;

        private BluetoothHandler(LoggerDataLoadActivity owner, ConsoleMessagesAppender consoleAppender) {
            super();
            this.owner = owner;
            this.consoleAppender = consoleAppender;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ThreadMessageTypes.MSG_CONSOLE) {
                consoleAppender.appendMessage((String) msg.obj);
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED) {
                owner.runningThread = null;
                owner.setControlsEnabled(true);
            }
        }
    }
}
