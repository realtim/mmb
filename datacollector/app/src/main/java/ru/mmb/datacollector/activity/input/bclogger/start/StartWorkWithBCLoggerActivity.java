package ru.mmb.datacollector.activity.input.bclogger.start;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.input.bclogger.ActivityStateWithScanPointAndLogger;
import ru.mmb.datacollector.activity.input.bclogger.select.SelectBCLoggerActivity;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_BCLOGGER_SELECT_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY;

public class StartWorkWithBCLoggerActivity extends Activity {
    private ActivityStateWithScanPointAndLogger currentState;

    private TextView labLogger;
    private Button btnSelectLogger;
    private Button btnSettings;
    private Button btnLoadData;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithScanPointAndLogger("input.bclogger.start");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_start);

        labLogger = (TextView) findViewById(R.id.inputBcloggerStart_loggerLabel);
        btnSelectLogger = (Button) findViewById(R.id.inputBcloggerStart_selectLoggerBtn);
        btnSettings = (Button) findViewById(R.id.inputBcloggerStart_settingsBtn);
        btnLoadData = (Button) findViewById(R.id.inputBcloggerStart_loadDataBtn);

        initializeBluetoothAdapter();

        btnSelectLogger.setOnClickListener(new SelectLoggerClickListener());
        //btnSettings.setOnClickListener(new SettingsClickListener());
        //btnLoadData.setOnClickListener(new LoadDataClickListener());

        refreshState();
    }

    private void initializeBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY);
        }
    }

    private void refreshState() {
        setTitle(currentState.getScanPointAndLoggerText(this));

        if (currentState.getCurrentLoggerInfo() != null)
            labLogger.setText(currentState.getCurrentLoggerInfo().getLoggerName());

        boolean canEnable = bluetoothAdapter.isEnabled() && currentState.isLoggerSelected();

        btnSelectLogger.setEnabled(bluetoothAdapter.isEnabled());
        btnSettings.setEnabled(canEnable);
        btnLoadData.setEnabled(canEnable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT_BCLOGGER_SELECT_ACTIVITY:
                onSelectBCLoggerActivityResult(resultCode, data);
                break;
            case REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY:
                onLaunchBluetoothActivityResult(resultCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onLaunchBluetoothActivityResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            long time_old = System.currentTimeMillis();
            while (!bluetoothAdapter.isEnabled()) {
                if (System.currentTimeMillis() - time_old > 5000) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        refreshState();
    }

    private void onSelectBCLoggerActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            currentState.loadFromIntent(data);
            refreshState();
        }
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
    }

    private class SelectLoggerClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), SelectBCLoggerActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_SELECT_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_INPUT_BCLOGGER_SELECT_ACTIVITY);
        }
    }

    /*
    private class SettingsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), BCLoggerSettingsActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY);
        }
    }

    private class LoadDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), BCLoggerLoadDataActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY);
        }
    }
    */
}
