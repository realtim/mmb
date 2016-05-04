package ru.mmb.loggermanager.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class BluetoothAdapterEnableActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter = null;

    @Override
    protected void onStart() {
        super.onStart();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            onAdapterStateChanged();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "TURN ON BT ADAPTER!", Toast.LENGTH_LONG).show();
        } else {
            onAdapterStateChanged();
        }
    }

    protected void onAdapterStateChanged() {
        // !!! override in subclasses
    }

    public boolean isAdapterEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
}
