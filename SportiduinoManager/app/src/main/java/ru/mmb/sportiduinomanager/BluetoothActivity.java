package ru.mmb.sportiduinomanager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.Station;

/**
 * Provides ability to discover a station, connect to it and set it's mode.
 */
public final class BluetoothActivity extends MainActivity implements BTDeviceListAdapter.OnItemClicked {
    /**
     * Code of started Bluetooth discovery activity.
     */
    private static final int REQUEST_ENABLE_BT = 1;
    /**
     * Bluetooth adapter state: hardware is absent.
     */
    private static final int BT_STATE_ABSENT = 0;
    /**
     * Bluetooth adapter state: it is turned off.
     */
    private static final int BT_STATE_OFF = 1;
    /**
     * Bluetooth adapter state: it is turned on.
     */
    private static final int BT_STATE_ON = 2;
    /**
     * Bluetooth device search is not active.
     */
    private static final int BT_SEARCH_OFF = 0;
    /**
     * Bluetooth device search is active.
     */
    private static final int BT_SEARCH_ON = 1;

    /**
     * RecyclerView with discovered Bluetooth devices and connect buttons.
     */
    private BTDeviceListAdapter mAdapter;

    /**
     * Reference to main thread object with all persistent data.
     */
    private MainApplication mMainApplication;
    /**
     * Bluetooth adapter handler.
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Current state of bluetooth adapter.
     */
    private int mBluetoothState = BT_STATE_ABSENT;
    /**
     * Current state of bluetooth device discovery.
     */
    private int mBluetoothSearch = BT_SEARCH_OFF;

    /**
     * Receiver of Bluetooth adapter state changes.
     */
    private final BroadcastReceiver mBTStateMonitor = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Get new Bluetooth state
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    if (mBluetoothState != BT_STATE_ON) {
                        // Bluetooth state was changed, update activity layout
                        mBluetoothState = BT_STATE_ON;
                        updateLayout(false);
                    }
                    break;
                case BluetoothAdapter.STATE_OFF:
                    if (mBluetoothState != BT_STATE_OFF) {
                        // Bluetooth state was changed, update activity layout
                        mBluetoothState = BT_STATE_OFF;
                        updateLayout(false);
                        // Ask to turn it on
                        final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Receiver of Bluetooth device discovery events.
     */
    private final BroadcastReceiver mSearchDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mBluetoothSearch = BT_SEARCH_OFF;
                updateLayout(false);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device
                // Get the BluetoothDevice object and its info from the Intent
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mAdapter.insertItem(device);
                final List<BluetoothDevice> deviceList = mMainApplication.getBTDeviceList();
                deviceList.add(device);
                mMainApplication.setBTDeviceList(deviceList);
            }
        }
    };

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mMainApplication = (MainApplication) getApplication();
        setContentView(R.layout.activity_bluetooth);
        updateMenuItems(mMainApplication, R.id.bluetooth);
        // Start monitoring bluetooth changes
        registerReceiver(mBTStateMonitor, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        // Prepare for Bluetooth device search
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mSearchDevices, filter);
        // Prepare recycler view of device list
        final RecyclerView recyclerView = findViewById(R.id.device_list);
        // use a linear layout manager
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // specify an RecyclerView adapter
        // and copy saved device list from main application
        mAdapter = new BTDeviceListAdapter(mMainApplication.getBTDeviceList(), this);
        final Station station = mMainApplication.getStation();
        if (station != null) mAdapter.setConnectedDevice(station.getAddress(), false);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.bluetooth).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Check if device supports Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            mBluetoothState = BT_STATE_ABSENT;
            updateLayout(false);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_bt_absent),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Check if Bluetooth was turned on
        try {
            if (!mBluetoothAdapter.isEnabled()) {
                // Hide all elements and request Bluetooth
                mBluetoothState = BT_STATE_OFF;
                updateLayout(false);
                final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
            }
        } catch (SecurityException e) {
            // Bluetooth permission was withdrawn from the application
            mBluetoothState = BT_STATE_ABSENT;
            updateLayout(false);
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_bt_forbidden),
                    Toast.LENGTH_LONG).show();
            return;
        }
        mBluetoothState = BT_STATE_ON;
        // Get Bluetooth search state
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothSearch = BT_SEARCH_ON;
        } else {
            mBluetoothSearch = BT_SEARCH_OFF;
        }
        // Get list of active points (if a distance was downloaded)
        final Distance distance = mMainApplication.getDistance();
        List<String> points = new ArrayList<>();
        if (distance != null) {
            points = distance.getPointNames(getResources().getString(R.string.active_point_prefix));
        }
        // Create list adapter for station mode spinner
        final Spinner spinner = findViewById(R.id.station_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, points);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        // Update activity layout
        updateLayout(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister Bluetooth state monitor
        unregisterReceiver(mBTStateMonitor);
        unregisterReceiver(mSearchDevices);
    }

    /**
     * Update private mBluetoothState on result of "Turn BT On" request.
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode != REQUEST_ENABLE_BT) return;
        if (resultCode == RESULT_OK) {
            mBluetoothState = BT_STATE_ON;
        } else {
            mBluetoothState = BT_STATE_OFF;
        }
        updateLayout(false);
    }

    /**
     * The onClick implementation of the RecyclerView item click.
     */
    @Override
    public void onItemClick(final int position) {
        final BluetoothDevice deviceClicked = mAdapter.getDevice(position);
        // Cancel Bluetooth discovery process
        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        // Disconnect from previous station
        final Station station = mMainApplication.getStation();
        if (station != null) {
            station.disconnect();
            mMainApplication.setStation(null);
        }
        // Mark clicked device as being connected
        mAdapter.setConnectedDevice(deviceClicked.getAddress(), true);
        // Try to connect to device in background thread
        new ConnectDevice(this).execute(deviceClicked);
    }

    /**
     * Search for available Bluetooth devices.
     *
     * @param view View of button clicked
     */
    public void searchForDevices(final View view) {
        // Don't try to search without Bluetooth
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            // Hide all elements and request Bluetooth
            mBluetoothState = BT_STATE_OFF;
            updateLayout(false);
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        // In Android >= 6.0 you have to ask for the runtime permission as well
        // in order for the discovery to get the devices ids. If you don't do this,
        // the discovery won't find any device.
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        // Empty device list in view and in the app
        mMainApplication.setBTDeviceList(new ArrayList<>());
        mAdapter.clearList();
        // Disconnect currently connected station
        final Station station = mMainApplication.getStation();
        if (station != null) {
            station.disconnect();
            mMainApplication.setStation(station);
        }
        // Start searching, API will return false if BT is turned off
        // (should not be the case but check it anyway)
        if (!mBluetoothAdapter.startDiscovery()) return;
        // Display progress indicator
        mBluetoothSearch = BT_SEARCH_ON;
        updateLayout(false);
    }

    /**
     * Set new mode and number for the station.
     *
     * @param view View of button clicked
     */
    public void changeStationMode(final View view) {
        // Get the station we work with
        final Station station = mMainApplication.getStation();
        if (station == null) return;
        // Get the distance
        final Distance distance = mMainApplication.getDistance();
        if (distance == null) {
            Toast.makeText(getApplicationContext(), R.string.err_db_no_distance_loaded, Toast.LENGTH_LONG).show();
            return;
        }
        // Get new station number
        final Spinner spinner = findViewById(R.id.station_spinner);
        final byte newNumber = (byte) spinner.getSelectedItemPosition();
        // Compute new station mode
        byte newMode;
        switch (distance.getPointType(newNumber)) {
            case -1:
                Toast.makeText(getApplicationContext(), R.string.err_bt_bad_point_number,
                        Toast.LENGTH_LONG).show();
                return;
            case 0:
                newMode = Station.MODE_INIT_CHIPS;
                break;
            case 2:
                newMode = Station.MODE_FINISH_POINT;
                break;
            default:
                newMode = Station.MODE_OTHER_POINT;
                break;
        }
        // Do nothing if numbers are the same
        final byte currentNumber = station.getNumber();
        if (currentNumber == newNumber && newMode == station.getMode()) return;
        // TODO: Check if some teams data from station was not downloaded and download it
        // Reset station to change it's number
        // TODO: Show hourglass or something, reset takes long time
        if (currentNumber != newNumber && !station.resetStation(newNumber)) {
            Toast.makeText(getApplicationContext(), station.getLastError(), Toast.LENGTH_LONG).show();
            return;
        }
        long responseTime = station.getResponseTime();
        // Set new station mode
        if (!station.newMode(newMode)) {
            Toast.makeText(getApplicationContext(), station.getLastError(), Toast.LENGTH_LONG).show();
            updateLayout(false);
            updateMenuItems(mMainApplication, R.id.bluetooth);
            ((TextView) findViewById(R.id.station_bt_response)).setText(getResources()
                    .getString(R.string.response_time, responseTime + station.getResponseTime()));
            return;
        }
        // Compute total response time for two calls
        responseTime += station.getResponseTime();
        // Get current station status (just in case) and update layout
        updateLayout(true);
        updateMenuItems(mMainApplication, R.id.bluetooth);
        responseTime += station.getResponseTime();
        // Set correct response time for sum of three commands responses
        ((TextView) findViewById(R.id.station_bt_response)).setText(getResources()
                .getString(R.string.response_time, responseTime));
    }

    /**
     * Synchronize station clock with Android clock.
     *
     * @param view View of button clicked
     */
    public void syncStationClock(final View view) {
        final Station station = mMainApplication.getStation();
        if (station == null) return;
        if (station.syncTime()) {
            ((TextView) findViewById(R.id.station_bt_response)).setText(getResources()
                    .getString(R.string.response_time, station.getResponseTime()));
            ((TextView) findViewById(R.id.station_time_drift)).setText(getResources()
                    .getString(R.string.station_time_drift, station.getTimeDrift()));
        } else {
            Toast.makeText(getApplicationContext(), station.getLastError(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Update layout according to activity state.
     *
     * @param fetchStatus True if we need to send command to station
     *                    to get it's current status
     */
    private void updateLayout(final boolean fetchStatus) {
        // Show BT search button / progress
        if (mBluetoothSearch == BT_SEARCH_OFF) {
            findViewById(R.id.device_search_progress).setVisibility(View.INVISIBLE);
            findViewById(R.id.device_search).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.device_search).setVisibility(View.INVISIBLE);
            findViewById(R.id.device_search_progress).setVisibility(View.VISIBLE);
        }

        // If Bluetooth is absent/disabled - hide everything else
        if (mBluetoothState == BT_STATE_ABSENT || mBluetoothState == BT_STATE_OFF) {
            // Bluetooth is not working right now, disable everything
            findViewById(R.id.device_list).setVisibility(View.GONE);
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.device_list).setVisibility(View.VISIBLE);

        // Don'try to update station status during BT search
        if (mBluetoothSearch == BT_SEARCH_ON) {
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }

        // Show station status block
        final Station station = mMainApplication.getStation();
        // Station was not connected yet
        if (station == null) {
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }
        // Update station data if asked
        if (fetchStatus && !station.fetchStatus()) {
            Toast.makeText(getApplicationContext(), station.getLastError(),
                    Toast.LENGTH_LONG).show();
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }
        // Update station data status in layout
        ((TextView) findViewById(R.id.station_bt_name)).setText(station.getName());
        ((TextView) findViewById(R.id.station_bt_response)).setText(getResources()
                .getString(R.string.response_time, station.getResponseTime()));
        switch (station.getMode()) {
            case Station.MODE_INIT_CHIPS:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_0, station.getNumber()));
                break;
            case Station.MODE_OTHER_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_1, station.getNumber()));
                break;
            case Station.MODE_FINISH_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_2, station.getNumber()));
                break;
            default:
                ((TextView) findViewById(R.id.station_mode_value))
                        .setText(R.string.station_mode_unknown);
                break;
        }
        final Spinner spinner = findViewById(R.id.station_spinner);
        spinner.setSelection(station.getNumber());
        ((TextView) findViewById(R.id.station_time_drift)).setText(getResources()
                .getString(R.string.station_time_drift, station.getTimeDrift()));
        ((TextView) findViewById(R.id.station_chips_registered_value))
                .setText(String.format(Locale.getDefault(), "%d", station.getChipsRegistered()));
        ((TextView) findViewById(R.id.station_last_chip_time)).setText(station.getLastChipTimeString());
        // Show status block
        findViewById(R.id.station_status).setVisibility(View.VISIBLE);
    }

    /**
     * Separate thread for async connecting to Bluetooth device.
     */
    private static class ConnectDevice extends AsyncTask<BluetoothDevice, Void, Boolean> {
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
        ConnectDevice(final BluetoothActivity context) {
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
                    activity.mAdapter.setConnectedDevice(null, false);
                } else {
                    activity.mAdapter.setConnectedDevice(station.getAddress(), false);
                }
            } else {
                activity.mAdapter.setConnectedDevice(null, false);
            }
            // Update activity layout
            activity.updateLayout(true);
            activity.updateMenuItems(mMainApplication, R.id.bluetooth);
        }
    }
}
