package ru.mmb.sportiduinomanager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.task.ConnectDeviceTask;
import ru.mmb.sportiduinomanager.task.ResetStationTask;

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
     * Station reset not running now.
     */
    public static final int RESET_STATION_OFF = 0;
    /**
     * Station reset is in progress.
     */
    public static final int RESET_STATION_ON = 1;

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
     * Current state of station reset procedure.
     */
    private int mResetStation = RESET_STATION_OFF;
    /**
     * Reference to new point selection listener
     * gives us ability to enable/disable it.
     */
    private PointSelectedListener mOnPointSelected;

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
                if (!deviceList.contains(device)) {
                    deviceList.add(device);
                }
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
        Log.d("BT_ACTIVITY", "starting activity");
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.bluetooth).setChecked(true);
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Initialize points and modes spinners
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        pointSpinner.setAdapter(getPointsAdapter());
        mOnPointSelected = new PointSelectedListener();
        pointSpinner.setOnItemSelectedListener(mOnPointSelected);
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        modeSpinner.setAdapter(getModesAdapter());
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
        // Update activity layout
        updateLayout(true);
    }

    /**
     * Generator of list content for points dropdown list.
     *
     * @return Sorted list of all points of the distance
     */
    private ArrayAdapter<String> getPointsAdapter() {
        // Get list of active points (if a distance was downloaded)
        final Distance distance = mMainApplication.getDistance();
        List<String> points = new ArrayList<>();
        if (distance != null) {
            points = distance.getPointNames(getResources().getString(R.string.active_point_prefix));
        }
        return new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, points);
    }

    /**
     * Generator of list content for station modes dropdown list.
     *
     * @return List of all supported station modes
     */
    private ArrayAdapter<String> getModesAdapter() {
        final List<String> modes = Arrays.asList(
                getResources().getString(R.string.station_mode_0),
                getResources().getString(R.string.station_mode_1),
                getResources().getString(R.string.station_mode_2));
        return new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, modes);
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
            updateMenuItems(mMainApplication, R.id.bluetooth);
            // If connected station is clicked, then just disconnect it
            if (station.getAddress().equals(deviceClicked.getAddress())) {
                mAdapter.setConnectedDevice(null, false);
                // Remove station info
                updateLayout(false);
                return;
            }
        }
        // Mark clicked device as being connected
        mAdapter.setConnectedDevice(deviceClicked.getAddress(), true);
        // Try to connect to device in background thread
        new ConnectDeviceTask(this).execute(deviceClicked);
    }

    /**
     * Get device list adapter for ConnectDeviceTask.
     *
     * @return local device list adapter
     */
    public BTDeviceListAdapter getDeviceListAdapter() {
        return mAdapter;
    }

    /**
     * Accessor method to change mResetStation from AsyncTask.
     *
     * @param newState new state RESET_STATION_ON or RESET_STATION_OFF
     */
    public void setResetStationState(final int newState) {
        mResetStation = newState;
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
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        final byte newNumber = (byte) pointSpinner.getSelectedItemPosition();
        // Get new station mode
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        final byte newMode = (byte) modeSpinner.getSelectedItemPosition();
        // Do nothing if numbers are the same
        final byte currentNumber = station.getNumber();
        if (currentNumber == newNumber && newMode == station.getMode()) return;
        // TODO: Check if some teams data from station was not downloaded and download it
        // Reset station to change it's number
        if (currentNumber != newNumber) {
            new ResetStationTask(this, newMode).execute(newNumber, newMode);
            return;
        }
        // If no station reset needed, then just call station mode change
        onStationNumberReset(newMode);
    }

    /**
     * Call station mode change when station reset finished.
     * Perform all short-running operations on station mode change
     * right in main UI thread.
     *
     * @param newMode new station mode to set
     */
    public void onStationNumberReset(final byte newMode) {
        final Station station = mMainApplication.getStation();
        if (station == null) return;
        long responseTime = station.getResponseTime();
        // Set new station mode
        if (newMode != station.getMode()) {
            if (!station.newMode(newMode)) {
                Toast.makeText(getApplicationContext(), station.getLastError(), Toast.LENGTH_LONG).show();
                updateLayout(false);
                updateMenuItems(mMainApplication, R.id.bluetooth);
                ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
                        .getString(R.string.response_time, responseTime + station.getResponseTime()));
                return;
            }
            // Compute total response time for two calls
            responseTime += station.getResponseTime();
        }
        // Get current station status (just in case) and update layout
        updateLayout(true);
        updateMenuItems(mMainApplication, R.id.bluetooth);
        responseTime += station.getResponseTime();
        // Set correct response time for sum of three commands responses
        ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
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
            ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
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
    public void updateLayout(final boolean fetchStatus) {
        // Show BT search button / progress
        if (mBluetoothSearch == BT_SEARCH_OFF && mResetStation == RESET_STATION_OFF) {
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

        // Don'try to update station status during BT search or running station reset
        if (mBluetoothSearch == BT_SEARCH_ON || mResetStation == RESET_STATION_ON) {
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
        if (fetchStatus && (!station.fetchConfig() || !station.fetchStatus())) {
            Toast.makeText(getApplicationContext(), station.getLastError(),
                    Toast.LENGTH_LONG).show();
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }
        // Update station data status in layout
        ((TextView) findViewById(R.id.station_bt_name)).setText(station.getName());
        ((TextView) findViewById(R.id.station_firmware)).setText(getResources()
                .getString(R.string.station_firmware, station.getFirmware()));
        ((TextView) findViewById(R.id.station_voltage)).setText(getResources()
                .getString(R.string.station_voltage,
                        station.getVoltage(), station.getTemperature()));
        ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
                .getString(R.string.response_time, station.getResponseTime()));
        final Distance distance = mMainApplication.getDistance();
        String pointName;
        if (distance == null) {
            pointName = "#" + station.getNumber();
        } else {
            pointName = distance.getPointName(station.getNumber(),
                    getResources().getString(R.string.active_point_prefix));
        }
        switch (station.getMode()) {
            case Station.MODE_INIT_CHIPS:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_0, pointName));
                break;
            case Station.MODE_OTHER_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_1, pointName));
                break;
            case Station.MODE_FINISH_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_2, pointName));
                break;
            default:
                ((TextView) findViewById(R.id.station_mode_value))
                        .setText(R.string.station_mode_unknown);
                break;
        }

        // Update drop down lists according to current station number and mode
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        // Disable point spinner selection listener during this update
        mOnPointSelected.setCalledFromUpdate();
        pointSpinner.setSelection(station.getNumber());
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        modeSpinner.setSelection(station.getMode());

        ((TextView) findViewById(R.id.station_time_drift)).setText(getResources()
                .getString(R.string.station_time_drift, station.getTimeDrift()));
        ((TextView) findViewById(R.id.station_chips_registered_value))
                .setText(String.format(Locale.getDefault(), "%d", station.getChipsRegistered()));
        ((TextView) findViewById(R.id.station_last_chip_time)).setText(station.getLastChipTimeString());
        // Show status block
        findViewById(R.id.station_status).setVisibility(View.VISIBLE);
    }

    /**
     * Callback to be invoked when an item in points list has been selected.
     */
    class PointSelectedListener implements AdapterView.OnItemSelectedListener {

        /**
         * Flag for disabling Mode Spinner change when called from updateLayout.
         */
        private boolean mCalledFromUpdate;

        /**
         * Set mCalledFromUpdate to true for
         * disabling setting mode to default during layout update.
         */
        void setCalledFromUpdate() {
            this.mCalledFromUpdate = true;
        }

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
                                   final long rowId) {
            // Disable listener when called directly at updateLayout()
            if (!mCalledFromUpdate) {
                final int defaultMode = getPointDefaultMode(parent.getSelectedItemPosition());
                if (defaultMode >= 0) {
                    ((Spinner) findViewById(R.id.station_mode_spinner)).setSelection(defaultMode);
                }
            }
            mCalledFromUpdate = false;
        }

        /**
         * Get default mode for selected point.
         *
         * @param pointNumber Point number in downloaded distance
         * @return One of Station.MODE_* constants or -1 in case of error
         */
        private int getPointDefaultMode(final int pointNumber) {
            final Distance distance = mMainApplication.getDistance();
            if (distance == null) return -1;
            switch (distance.getPointType(pointNumber)) {
                case -1:
                    return -1;
                case 0:
                    return Station.MODE_INIT_CHIPS;
                case 2:
                    return Station.MODE_FINISH_POINT;
                default:
                    return Station.MODE_OTHER_POINT;
            }
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            // do nothing
        }
    }
}
