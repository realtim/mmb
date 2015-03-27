package ru.mmb.datacollector.activity.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY;

public class BluetoothAdapterEnableActivity extends Activity {
    private BluetoothAdapter bluetoothAdapter = null;

    @Override
    protected void onStart() {
        super.onStart();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY);
        } else {
            onAdapterStateChanged();
        }
    }

    protected void onAdapterStateChanged() {
        // TODO override in subclasses
    }

    public boolean isAdapterEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY:
                onLaunchBluetoothActivityResult(resultCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onLaunchBluetoothActivityResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            long timeOld = System.currentTimeMillis();
            while (!bluetoothAdapter.isEnabled()) {
                if (System.currentTimeMillis() - timeOld > 5000) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
        onAdapterStateChanged();
    }
}
