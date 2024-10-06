package ru.mmb.sportiduinomanager.task;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.BluetoothActivity;
import ru.mmb.sportiduinomanager.MainApp;
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
     * Retain only a weak reference to the activity.
     *
     * @param context Context of calling activity
     */
    public ConnectDeviceTask(final BluetoothActivity context) {
        super();
        mActivityRef = new WeakReference<>(context);
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
        // Get a reference to the activity if it is still there
        final BluetoothActivity activity = mActivityRef.get();
        if (activity == null || activity.isFinishing()) return;
        // Process reset result in UI activity
        activity.onStationConnectResult(result);
    }
}
