package ru.mmb.sportiduinomanager.task;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.BluetoothActivity;
import ru.mmb.sportiduinomanager.MainApp;
import ru.mmb.sportiduinomanager.R;
import ru.mmb.sportiduinomanager.adapter.BTDeviceListAdapter;
import ru.mmb.sportiduinomanager.model.StationAPI;

/**
 * Separate thread for async connecting to Bluetooth device.
 */
public class ConnectDeviceTask extends AsyncTask<BluetoothDevice, Void, Integer> {
    /**
     * Reference to parent activity (which can cease to exist in any moment).
     */
    private final WeakReference<BluetoothActivity> mActivityRef;
    /**
     * Reference to main application thread.
     */
    private final MainApp mMainApplication;
    /**
     * RecyclerView with discovered Bluetooth devices and connect buttons.
     */
    private final BTDeviceListAdapter mAdapter;

    /**
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     * @param adapter RecyclerView adapter with device list
     */
    public ConnectDeviceTask(final BluetoothActivity context, final BTDeviceListAdapter adapter) {
        super();
        mActivityRef = new WeakReference<>(context);
        mMainApplication = (MainApp) context.getApplication();
        mAdapter = adapter;
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
    protected Integer doInBackground(final BluetoothDevice... device) {
        final StationAPI station = new StationAPI(device[0]);
        if (station.connect()) {
            // Save connected station in main application
            MainApp.setStation(station);
            // Set all station class instance variables
            // by sending getConfig and getStatus commands to station
            if (!MainApp.mStation.fetchConfig()) return MainApp.mStation.getLastError(true);
            if (!MainApp.mStation.fetchStatus()) return MainApp.mStation.getLastError(true);
            // Create filtered list of punches at station number control point
            MainApp.setPointPunches(MainApp.mAllRecords.getPunchesAtStation(MainApp.mStation.getNumber(),
                    MainApp.mStation.getMACasLong()));
            return 0;
        }
        return -1;
    }

    /**
     * Show error message in case of connect failure and update screen layout.
     *
     * @param result False if connection attempt failed
     */
    protected void onPostExecute(final Integer result) {
        // Show error message if connect attempt failed
        if (result < 0) {
            // Connection to BT device failed
            Toast.makeText(mMainApplication, R.string.err_bt_cant_connect, Toast.LENGTH_LONG).show();
        } else if (result > 0) {
            // Station responded to getConfig/getStatus commands with an error,
            // show this error to user
            Toast.makeText(mMainApplication, result, Toast.LENGTH_LONG).show();
        }
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Update device list in activity
        if (result == 0) {
            if (MainApp.mStation == null) {
                mAdapter.setConnectedDevice(null, false);
            } else {
                mAdapter.setConnectedDevice(MainApp.mStation.getAddress(), false);
            }
        } else {
            mAdapter.setConnectedDevice(null, false);
        }
        // Update activity layout
        activity.updateLayout(false);
        // Update menu items only after station status request
        activity.updateMenuItems(R.id.bluetooth);
    }
}
