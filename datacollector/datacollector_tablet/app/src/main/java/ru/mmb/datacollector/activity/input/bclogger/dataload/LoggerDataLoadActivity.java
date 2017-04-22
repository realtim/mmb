package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.widget.ConsoleMessagesAppender;

public class LoggerDataLoadActivity extends Activity {
    ActivityStateWithScanPointAndBTDevice currentState;

    private Button btnGetLog;
    private Button btnGetErrors;
    private Button btnClearConsole;
    // private Button btnClearDevice;
    private TextView areaConsole;

    private ConsoleMessagesAppender consoleAppender;
    private LoggerDataLoadBluetoothClient bluetoothClient;
    private Thread runningThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithScanPointAndBTDevice("input.bclogger.dataload");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_dataload);

        btnGetLog = (Button) findViewById(R.id.inputBCLoggerDataload_getLogButton);
        btnGetErrors = (Button) findViewById(R.id.inputBCLoggerDataload_getErrorsButton);
        btnClearConsole = (Button) findViewById(R.id.inputBCLoggerDataload_clearConsoleButton);
        // btnClearDevice = (Button) findViewById(R.id.inputBCLoggerDataload_clearDeviceButton);
        areaConsole = (TextView) findViewById(R.id.inputBCLoggerDataload_consoleTextView);

        setTitle(currentState.getScanPointAndDeviceText(this));

        btnGetLog.setOnClickListener(new GetLogClickListener());
        btnGetErrors.setOnClickListener(new GetErrorsClickListener());
        btnClearConsole.setOnClickListener(new ClearConsoleClickListener());
        // btnClearDevice.setOnClickListener(new ClearDeviceClickListener());

        consoleAppender = new ConsoleMessagesAppender(areaConsole);
        Handler bluetoothHandler = new BluetoothHandler(this, consoleAppender);
        bluetoothClient = new LoggerDataLoadBluetoothClient(this, currentState.getCurrentDeviceInfo(), currentState.getCurrentScanPoint(), bluetoothHandler);

        setControlsEnabled(true);
    }

    private void setControlsEnabled(boolean value) {
        btnGetLog.setEnabled(value);
        btnGetErrors.setEnabled(value);
        btnClearConsole.setEnabled(value);
        // btnClearDevice.setEnabled(value);
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

    private class GetLogClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(false);

            // backup database before logger data import
            SQLiteDatabaseAdapter dbAdapter = SQLiteDatabaseAdapter.getConnectedInstance();
            dbAdapter.backupDatabase(LoggerDataLoadActivity.this);

            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    bluetoothClient.loadLogData();
                }
            });
            runningThread.start();
        }
    }

    private class GetErrorsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(false);
            runningThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    bluetoothClient.loadErrorsData();
                }
            });
            runningThread.start();
        }
    }

    private class ClearConsoleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            consoleAppender.clear();
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
            } else if (msg.what == ThreadMessageTypes.MSG_FINISHED_SUCCESS) {
                owner.runningThread = null;

                // backup database after logger data import
                SQLiteDatabaseAdapter dbAdapter = SQLiteDatabaseAdapter.getConnectedInstance();
                dbAdapter.backupDatabase(owner);

                owner.setControlsEnabled(true);
            }
        }
    }
}
