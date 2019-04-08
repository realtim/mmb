package ru.mmb.sportiduinomanager.activity.devices;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.MainApplication;
import ru.mmb.sportiduinomanager.R;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Separate thread for async connecting to Bluetooth device.
 */
public class ConnectDeviceTask extends AsyncTask<BluetoothDevice, Void, Boolean> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<BluetoothActivity> mActivityRef;
    /**
     * Reference to main application thread.
     */
    private final MainApplication mMainApplication;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ConnectDeviceTask(final BluetoothActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApplication) context.getApplication();
    }

    /**
     * Show hourglass icon before connecting to the device.
     */
    protected void onPreExecute() {
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Update activity layout
        activity.updateLayout(false);
    }

    /**
     * Try to connect to the Bluetooth device.
     *
     * @param device Bluetooth device clicked
     * @return True if succeeded
     */
    protected Boolean doInBackground(final BluetoothDevice... device) {
        final Station station = new Station(device[0]);
        if (station.connect()) {
            // Save connected station in main application
            mMainApplication.setStation(station);
            return true;
        }
        // Disconnect from station
        station.disconnect();
        mMainApplication.setStation(null);
        return false;
    }

    /**
     * Show error message in case of connect failure and update screen layout.
     *
     * @param result False if connection attempt failed
     */
    protected void onPostExecute(final Boolean result) {
        // Show error message if connect attempt failed
        if (!result) {
            Toast.makeText(mMainApplication, R.string.err_bt_cant_connect, Toast.LENGTH_LONG).show();
        }
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Update device list in activity
        if (result) {
            final Station station = mMainApplication.getStation();
            if (station == null) {
                activity.getDeviceListAdapter().setConnectedDevice(null, false);
            } else {
                activity.getDeviceListAdapter().setConnectedDevice(station.getAddress(), false);
            }
        } else {
            activity.getDeviceListAdapter().setConnectedDevice(null, false);
        }
        // Update activity layout
        activity.updateLayout(true);
        // Update menu items only after station status request
        activity.fireUpdateMenuItems();
    }
}
