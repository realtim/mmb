package ru.mmb.datacollector.activity.input.bclogger.start;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;
import ru.mmb.datacollector.activity.bluetooth.BluetoothAdapterEnableActivity;
import ru.mmb.datacollector.activity.bluetooth.BluetoothSelectDeviceActivity;
import ru.mmb.datacollector.activity.input.bclogger.dataload.LoggerDataLoadActivity;
import ru.mmb.datacollector.activity.input.bclogger.settings.LoggerSettingsActivity;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY;

public class StartWorkWithBCLoggerActivity extends BluetoothAdapterEnableActivity {
    private ActivityStateWithScanPointAndBTDevice currentState;

    private TextView labLogger;
    private Button btnSelectLogger;
    private Button btnSettings;
    private Button btnLoadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithScanPointAndBTDevice("input.bclogger.start");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.input_bclogger_start);

        labLogger = (TextView) findViewById(R.id.inputBcloggerStart_loggerLabel);
        btnSelectLogger = (Button) findViewById(R.id.inputBcloggerStart_selectLoggerBtn);
        btnSettings = (Button) findViewById(R.id.inputBcloggerStart_settingsBtn);
        btnLoadData = (Button) findViewById(R.id.inputBcloggerStart_loadDataBtn);

        btnSelectLogger.setOnClickListener(new SelectLoggerClickListener());
        btnSettings.setOnClickListener(new SettingsClickListener());
        btnLoadData.setOnClickListener(new LoadDataClickListener());

        refreshState();
    }

    private void refreshState() {
        setTitle(currentState.getScanPointAndDeviceText(this));

        if (currentState.getCurrentDeviceInfo() != null)
            labLogger.setText(currentState.getCurrentDeviceInfo().getDeviceName());

        boolean canEnable = isAdapterEnabled() && currentState.isDeviceSelected();

        btnSelectLogger.setEnabled(isAdapterEnabled());
        btnSettings.setEnabled(canEnable);
        btnLoadData.setEnabled(canEnable);
    }

    @Override
    protected void onAdapterStateChanged() {
        refreshState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY:
                onSelectBCLoggerActivityResult(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
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
            Intent intent = new Intent(getApplicationContext(), BluetoothSelectDeviceActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY);
            intent.putExtra(KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS, true);
            startActivityForResult(intent, REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY);
        }
    }

    private class SettingsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), LoggerSettingsActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY);
            startActivity(intent);
        }
    }

    private class LoadDataClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), LoggerDataLoadActivity.class);
            currentState.prepareStartActivityIntent(intent, REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY);
            startActivityForResult(intent, REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY);
        }
    }
}
