package ru.mmb.loggermanager.activity.timeupdater;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ru.mmb.loggermanager.bluetooth.DeviceInfo;

public class DevicesLoader {
    public static List<DeviceInfo> loadPairedDevices() {
        List<DeviceInfo> result = new ArrayList<DeviceInfo>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            if (device.getName().contains("LOGGER")) {
                result.add(new DeviceInfo(device.getName(), device.getAddress()));
            }
        }
        Collections.sort(result);
        return result;
    }
}
