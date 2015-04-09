package ru.mmb.datacollector.activity.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;
import ru.mmb.datacollector.activity.StateChangeListener;
import ru.mmb.datacollector.bluetooth.DeviceInfo;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_DEVICE_INFO;

/*
 * To start this activity bluetooth adapter MUST be enabled.
 * Check enabled state in calling activity.
 */
public class BluetoothSelectDeviceActivity extends Activity implements StateChangeListener {
    private ActivityStateWithScanPointAndBTDevice currentState;

    private Map<String, DeviceInfo> pairedDevices;
    private List<String> deviceNames;

    private Spinner inputDevice;
    private Button btnOk;

    private String prevSelectedDevice = null;
    private String currSelectedDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getInstance().setCurrentContext(this);

        currentState = new ActivityStateWithScanPointAndBTDevice("bluetooth.device.select");
        currentState.initialize(this, savedInstanceState);

        setContentView(R.layout.bluetooth_select_device);

        inputDevice = (Spinner) findViewById(R.id.bluetoothSelectDevice_deviceInput);
        btnOk = (Button) findViewById(R.id.bluetoothSelectDevice_okBtn);

        loadPairedDevicesList();
        buildDeviceNamesArray();
        setInputDeviceAdapter();

        inputDevice.setOnItemSelectedListener(new InputDeviceOnItemSelectedListener());
        btnOk.setOnClickListener(new OkBtnClickListener());

        initializeControls();

        currentState.addStateChangeListener(this);
        onStateChange();
    }

    private void loadPairedDevicesList() {
        pairedDevices = new TreeMap<String, DeviceInfo>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            boolean needAddDevice =
                    !currentState.isFilterJustLoggers() || device.getName().contains("LOGGER");
            if (needAddDevice) {
                pairedDevices.put(device.getName(), new DeviceInfo(device.getName(), device.getAddress()));
            }
        }
    }

    private void buildDeviceNamesArray() {
        deviceNames = new ArrayList<String>();
        for (String loggerName : pairedDevices.keySet()) {
            deviceNames.add(loggerName);
        }
    }

    private void setInputDeviceAdapter() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputDevice.setAdapter(adapter);
    }

    private void initializeControls() {
        refreshInputDeviceState();

        if (currentState.getCurrentDeviceInfo() == null)
            currentState.setCurrentDeviceInfo(pairedDevices.get(deviceNames.get(0)));
        DeviceInfo currentDeviceInfo = currentState.getCurrentDeviceInfo();
        setInitialDeviceName(currentDeviceInfo);
    }

    private void refreshInputDeviceState() {
        if (currentState.getCurrentDeviceInfo() == null) {
            inputDevice.setSelection(0);
        } else {
            int pos = deviceNames.indexOf(currentState.getCurrentDeviceInfo().getDeviceName());
            if (pos == -1) pos = 0;
            inputDevice.setSelection(pos);
        }
    }

    private void setInitialDeviceName(DeviceInfo currentDeviceInfo) {
        currSelectedDevice = currentDeviceInfo.getDeviceName();
        prevSelectedDevice = currSelectedDevice;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        currentState.save(outState);
    }

    @Override
    public void onStateChange() {
        btnOk.setEnabled(currentState.isDeviceSelected());
    }

    private class InputDeviceOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            currSelectedDevice = deviceNames.get(pos);
            if (!currSelectedDevice.equals(prevSelectedDevice)) {
                prevSelectedDevice = currSelectedDevice;
                currentState.setCurrentDeviceInfo(pairedDevices.get(currSelectedDevice));
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    private class OkBtnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent resultData = new Intent();
            if (currentState.getCurrentDeviceInfo() != null)
                resultData.putExtra(KEY_CURRENT_DEVICE_INFO, currentState.getCurrentDeviceInfo());
            setResult(RESULT_OK, resultData);
            finish();
        }
    }
}
