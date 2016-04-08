package ru.mmb.loggermanager.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.loggermanager.R;
import ru.mmb.loggermanager.bluetooth.DeviceInfo;

public class SelectLoggerPanel {
    private final MainActivity owner;

    private Map<String, DeviceInfo> pairedDevices;
    private List<String> deviceNames;

    private Spinner selectLoggerSpinner;

    private String prevSelectedDevice = null;
    private String currSelectedDevice = null;

    public SelectLoggerPanel(MainActivity owner) {
        this.owner = owner;
        initialize();
    }

    private void initialize() {
        selectLoggerSpinner = (Spinner) owner.findViewById(R.id.main_selectLoggerSpinner);
        loadPairedDevicesList();
        buildDeviceNamesArray();
        setSelectLoggerAdapter();
        selectLoggerSpinner.setOnItemSelectedListener(new SelectLoggerOnItemSelectedListener(owner));
    }

    private void loadPairedDevicesList() {
        pairedDevices = new TreeMap<String, DeviceInfo>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName().contains("LOGGER")) {
                pairedDevices.put(device.getName(), new DeviceInfo(device.getName(), device.getAddress()));
            }
        }
    }

    private void buildDeviceNamesArray() {
        deviceNames = new ArrayList<String>();
        deviceNames.add(owner.getResources().getString(R.string.main_no_logger_selected));
        for (String loggerName : pairedDevices.keySet()) {
            deviceNames.add(loggerName);
        }
    }

    private void setSelectLoggerAdapter() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(owner, android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectLoggerSpinner.setAdapter(adapter);
    }

    private class SelectLoggerOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        private MainActivity owner;

        public SelectLoggerOnItemSelectedListener(MainActivity owner) {
            this.owner = owner;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            currSelectedDevice = deviceNames.get(pos);
            if (!currSelectedDevice.equals(prevSelectedDevice)) {
                prevSelectedDevice = currSelectedDevice;
                DeviceInfo selectedDevice = pairedDevices.get(currSelectedDevice);
                owner.selectedLoggerChanged(selectedDevice);
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }
}
