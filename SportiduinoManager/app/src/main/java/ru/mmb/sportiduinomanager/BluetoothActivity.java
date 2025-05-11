package ru.mmb.sportiduinomanager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.mmb.sportiduinomanager.adapter.BTDeviceListAdapter;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.StationAPI;
import ru.mmb.sportiduinomanager.task.ConnectDeviceTask;
import ru.mmb.sportiduinomanager.task.ResetStationTask;

/**
 * Provides ability to discover a station, connect to it and set it's mode.
 */
public final class BluetoothActivity extends MenuActivity implements BTDeviceListAdapter.OnItemClicked {
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
     * Station reset is not running now.
     */
    private static final int RESET_STATION_OFF = 0;
    /**
     * Station reset is in progress.
     */
    private static final int RESET_STATION_ON = 1;
    /**
     * RecyclerView with discovered Bluetooth devices and connect buttons.
     */
    private BTDeviceListAdapter mAdapter;

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
                        mResultLauncher.launch(enableBtIntent);
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
                if (device == null) return;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) return;
                final String name = device.getName();
                if (name != null && !name.matches("Sport.*")) return;
                final List<BluetoothDevice> deviceList = MainApp.UI_STATE.getBTDeviceList();
                if (deviceList.contains(device)) return;
                mAdapter.insertItem(device);
                deviceList.add(device);
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                usePin(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            }
        }
    };
    /**
     * Update private mBluetoothState on result of "Turn BT On" request.
     */
    final ActivityResultLauncher<Intent> mResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == BluetoothActivity.RESULT_OK) {
                    mBluetoothState = BT_STATE_ON;
                } else {
                    mBluetoothState = BT_STATE_OFF;
                }
                updateLayout(false);
            });

    private void usePin(final BluetoothDevice device) {
        final byte[] pinBytes = MainApp.mDistance.getBluetoothPin().getBytes(StandardCharsets.UTF_8);
        if (pinBytes.length == 0 || pinBytes.length > 16) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) return;
        device.setPin(pinBytes);
    }

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        setContentView(R.layout.activity_bluetooth);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start monitoring bluetooth changes
        registerReceiver(mBTStateMonitor, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        // Prepare for Bluetooth device search
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(mSearchDevices, filter);
        // Get default Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.bluetooth).setChecked(true);
        updateMenuItems(R.id.bluetooth);
        // Prepare recycler view of device list
        final RecyclerView recyclerView = findViewById(R.id.device_list);
        // use a linear layout manager
        final RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // Create a RecyclerView adapter and set it with saved device list from main app
        mAdapter = new BTDeviceListAdapter(MainApp.UI_STATE.getBTDeviceList(), this);
        if (MainApp.mStation != null) {
            mAdapter.setConnectedDevice(MainApp.mStation.getAddress(), false);
        }
        recyclerView.setAdapter(mAdapter);
        // Initialize points and modes spinners
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        pointSpinner.setAdapter(getPointsAdapter());
        final PointSelectedListener onPointSelected = new PointSelectedListener();
        pointSpinner.setOnItemSelectedListener(onPointSelected);
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        modeSpinner.setAdapter(getModesAdapter());
        // Request the necessary permissions at runtime
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        // Check if Bluetooth is turned on
        if (mBluetoothAdapter == null) {
            mBluetoothState = BT_STATE_ABSENT;
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_bt_absent),
                    Toast.LENGTH_LONG).show();
        } else {
            try {
                if (!mBluetoothAdapter.isEnabled()) {
                    // Hide all elements and request Bluetooth
                    mBluetoothState = BT_STATE_OFF;
                    updateLayout(false);
                    final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    mResultLauncher.launch(enableBtIntent);
                    return;
                }
                mBluetoothState = BT_STATE_ON;
            } catch (SecurityException e) {
                // Bluetooth permission was withdrawn from the application
                mBluetoothState = BT_STATE_ABSENT;
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_bt_forbidden),
                        Toast.LENGTH_LONG).show();
            }
        }
        // Can't do anything if the tablet/phone does not supports Bluetooth
        if (mBluetoothState == BT_STATE_ABSENT) {
            updateLayout(false);
            return;
        }
        // Check if app has the permission to search BT devices
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
            return;
        }
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
        // Get list of control points (if a distance was downloaded)
        final List<String> points =
                MainApp.mDistance.getPointNames(
                        getResources().getString(R.string.control_point_prefix));
        return new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, points);
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
        return new ArrayAdapter<>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, modes);
    }

    @Override
    protected void onDestroy() {
        // Unregister Bluetooth state monitor
        unregisterReceiver(mBTStateMonitor);
        unregisterReceiver(mSearchDevices);
        super.onDestroy();
    }

    /**
     * The onClick implementation of the RecyclerView item click.
     */
    @Override
    public void onItemClick(final int position) {
        final BluetoothDevice deviceClicked = mAdapter.getDevice(position);
        // Cancel Bluetooth discovery process
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) return;
        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        // Disconnect from previous station
        if (MainApp.mStation != null) {
            MainApp.mStation.disconnect();
            // If connected station is clicked, then just disconnect it
            if (MainApp.mStation.getAddress().equals(deviceClicked.getAddress())) {
                mAdapter.setConnectedDevice(null, false);
                MainApp.setStation(null);
                // Remove station info
                updateMenuItems(R.id.bluetooth);
                updateLayout(false);
                return;
            } else {
                MainApp.setStation(null);
                updateMenuItems(R.id.bluetooth);
            }
        }
        // Mark clicked device as being connected
        mAdapter.setConnectedDevice(deviceClicked.getAddress(), true);
        // Try to connect to device in background thread
        new ConnectDeviceTask(this).execute(deviceClicked);
    }

    /**
     * Search for available Bluetooth devices.
     *
     * @param view View of button clicked (unused)
     */
    public void searchForDevices(@SuppressWarnings("unused") final View view) {
        // Don't try to search without Bluetooth
        if (mBluetoothAdapter == null) return;
        if (!mBluetoothAdapter.isEnabled()) {
            // Hide all elements and request Bluetooth
            mBluetoothState = BT_STATE_OFF;
            updateLayout(false);
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mResultLauncher.launch(enableBtIntent);
            return;
        }
        // In Android >= 6.0 you have to ask for the runtime permission as well
        // in order for the discovery to get the devices ids. If you don't do this,
        // the discovery won't find any device.
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        // Empty device list in view and in the app
        MainApp.UI_STATE.setBTDeviceList(new ArrayList<>());
        mAdapter.clearList();
        // Disconnect currently connected station
        if (MainApp.mStation != null) {
            MainApp.mStation.disconnect();
            MainApp.setStation(null);
            updateMenuItems(R.id.bluetooth);
        }

        // Start searching, API will return false if BT is turned off
        // (should not be the case but check it anyway)
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (!mBluetoothAdapter.startDiscovery()) return;
        // Display progress indicator
        mBluetoothSearch = BT_SEARCH_ON;
        updateLayout(false);
    }

    /**
     * Called when an attempt to connect to a station is finished.
     *
     * @param result Result of previously called station connect task
     */
    public void onStationConnectResult(final int result) {
        // Show error message if connect attempt failed
        if (result < 0) {
            // Connection to BT device failed
            Toast.makeText(getApplicationContext(), R.string.err_bt_cant_connect, Toast.LENGTH_LONG).show();
        } else if (result > 0) {
            // Station responded to getConfig/getStatus commands with an error,
            // show this error to user
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }
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
        updateLayout(false);
        // Update menu items depending of connected station mode
        updateMenuItems(R.id.bluetooth);
    }

    /**
     * Set new mode and number for the station.
     *
     * @param view View of button clicked (unused)
     */
    public void changeStationMode(@SuppressWarnings("unused") final View view) {
        if (MainApp.mStation == null) return;
        // Get new station number
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        final int newNumber =
                MainApp.mDistance.getNumberFromPosition(pointSpinner.getSelectedItemPosition());
        // Get new station mode
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        final int newMode = modeSpinner.getSelectedItemPosition();
        // Do nothing if numbers are the same
        final int currentNumber = MainApp.mStation.getNumber();
        if (currentNumber == newNumber && newMode == MainApp.mStation.getMode()) return;
        if (currentNumber != newNumber) {
            // Change reset station state
            mResetStation = RESET_STATION_ON;
            // Update activity layout
            updateResetProgress(0, 24);
            updateLayout(false);
            // TODO: add wake lock
            // Reset station to change it's number (and mode if needed)
            new ResetStationTask(this).execute(newNumber, newMode);
            return;
        }
        // If no station reset is needed,
        // then just call station mode change and display result
        MainApp.mStation.newMode(newMode);
        onStationResetResult(MainApp.mStation.getLastError(true));
    }

    /**
     * Called when station reset of station mode change is finished.
     *
     * @param result Result of previously called station reset / mode change
     */
    public void onStationResetResult(final int result) {
        // Turn RESET_STATION UI mode off
        mResetStation = RESET_STATION_OFF;
        // Update layout and return if station become disconnected
        if (MainApp.mStation == null) {
            updateLayout(false);
            updateMenuItems(R.id.bluetooth);
            return;
        }
        // Create filtered list of punches for new station number
        MainApp.setPointPunches(MainApp.mAllRecords
                .getPunchesAtStation(MainApp.mStation.getNumber(), MainApp.mStation.getMACasLong()));
        // Save response time of last command (it'll be overwritten by getStatus)
        long responseTime = MainApp.mStation.getResponseTime();
        // Update layout
        if (result == 0) {
            // Make update with call to getStatus (just in case)
            updateLayout(true);
            updateMenuItems(R.id.bluetooth);
            responseTime += MainApp.mStation.getResponseTime();
        } else {
            updateLayout(false);
            updateMenuItems(R.id.bluetooth);
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }
        // Set correct response time as the sum of two last commands responses
        ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
                .getString(R.string.response_time, responseTime));
    }

    /**
     * Synchronize station clock with Android clock.
     *
     * @param view View of button clicked (unused)
     */
    public void syncStationClock(@SuppressWarnings("unused") final View view) {
        if (MainApp.mStation == null) return;
        if (MainApp.mStation.syncTime()) {
            ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
                    .getString(R.string.response_time, MainApp.mStation.getResponseTime()));
            ((TextView) findViewById(R.id.station_time_drift)).setText(getResources()
                    .getString(R.string.station_time_drift, MainApp.mStation.getTimeDrift()));
        } else {
            Toast.makeText(getApplicationContext(), MainApp.mStation.getLastError(true),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Update status of station reset process in activity layout.
     *
     * @param percents          Task percents already completed
     * @param secondsToComplete Estimated number of seconds to completion
     */
    public void updateResetProgress(final int percents, final int secondsToComplete) {
        ((ProgressBar) findViewById(R.id.station_reset_percents)).setProgress(percents);
        ((TextView) findViewById(R.id.station_reset_time)).setText(getResources()
                .getQuantityString(R.plurals.station_reset_time, secondsToComplete,
                        secondsToComplete));
    }

    /**
     * Update layout according to activity state.
     *
     * @param fetchStatus True if we need to send command to station
     *                    to get it's current status
     */
    public void updateLayout(final boolean fetchStatus) {
        // Hide station reset progress if station reset is not running
        if (mResetStation == RESET_STATION_OFF) {
            findViewById(R.id.station_reset_progress).setVisibility(View.GONE);
        }

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

        // If station reset is running - show it's progress and hide everything else
        if (mResetStation == RESET_STATION_ON) {
            findViewById(R.id.device_search).setVisibility(View.GONE);
            findViewById(R.id.device_search_progress).setVisibility(View.GONE);
            findViewById(R.id.device_list).setVisibility(View.GONE);
            findViewById(R.id.station_status).setVisibility(View.GONE);
            findViewById(R.id.station_reset_progress).setVisibility(View.VISIBLE);
            return;
        }

        // Don't try to update station status during BT search
        if (mBluetoothSearch == BT_SEARCH_ON) {
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }

        // Show station status block
        showStationStatus(fetchStatus);
    }

    /**
     * Update station status block in layout.
     *
     * @param fetchStatus True if we need to send command to station
     *                    to get it's current status
     */
    private void showStationStatus(final boolean fetchStatus) {
        // Station was not connected yet
        if (MainApp.mStation == null) {
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }
        // Wait for StationQuerying to stop
        MainApp.mStation.waitForQuerying2Stop();
        // Update station data if asked
        if (fetchStatus && !(MainApp.mStation.fetchConfig() && MainApp.mStation.fetchStatus())) {
            Toast.makeText(getApplicationContext(), MainApp.mStation.getLastError(true),
                    Toast.LENGTH_LONG).show();
            findViewById(R.id.station_status).setVisibility(View.GONE);
            return;
        }

        // Display station name
        final String name = MainApp.mStation.getName() + " (" + MainApp.mStation.getAddress() + ")";
        ((TextView) findViewById(R.id.station_bt_name)).setText(name);
        // Display station firmware
        final int firmware = MainApp.mStation.getFirmware();
        final TextView firmwareView = findViewById(R.id.station_firmware);
        firmwareView.setText(getResources().getString(R.string.station_firmware, firmware));
        if (firmware == StationAPI.API_FIRMWARE) {
            firmwareView.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            firmwareView.setTextColor(getResources().getColor(R.color.bg_secondary));
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.err_bt_wrong_firmware, firmware, StationAPI.API_FIRMWARE),
                    Toast.LENGTH_LONG).show();
        }
        // Display station voltage
        final float voltage = MainApp.mStation.getVoltage();
        final TextView voltageView = findViewById(R.id.station_voltage);
        voltageView.setText(getResources().getString(R.string.station_voltage, voltage,
                MainApp.mStation.getTemperature()));
        if (voltage <= StationAPI.BATTERY_LOW) {
            voltageView.setTextColor(getResources().getColor(R.color.bg_secondary));
        } else {
            voltageView.setTextColor(getResources().getColor(R.color.text_secondary));
        }
        // Display station response time
        ((TextView) findViewById(R.id.station_response_time)).setText(getResources()
                .getString(R.string.response_time, MainApp.mStation.getResponseTime()));
        // Display station number as point name and station mode
        final String pointName = MainApp.mDistance.getPointName(MainApp.mStation.getNumber(),
                getResources().getString(R.string.control_point_prefix));
        switch (MainApp.mStation.getMode()) {
            case StationAPI.MODE_INIT_CHIPS:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_0, pointName));
                break;
            case StationAPI.MODE_OTHER_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_1, pointName));
                break;
            case StationAPI.MODE_FINISH_POINT:
                ((TextView) findViewById(R.id.station_mode_value)).setText(getResources()
                        .getString(R.string.station_mode_value_2, pointName));
                break;
            default:
                ((TextView) findViewById(R.id.station_mode_value))
                        .setText(R.string.station_mode_unknown);
                break;
        }

        // Update drop down lists according to current station number and default mode
        final Spinner pointSpinner = findViewById(R.id.station_point_spinner);
        final int position = MainApp.mDistance.getPositionFromNumber(MainApp.mStation.getNumber());
        pointSpinner.setSelection(position);
        final Spinner modeSpinner = findViewById(R.id.station_mode_spinner);
        modeSpinner.setSelection(MainApp.mStation.getMode());

        ((TextView) findViewById(R.id.station_time_drift)).setText(getResources()
                .getString(R.string.station_time_drift, MainApp.mStation.getTimeDrift()));
        ((TextView) findViewById(R.id.station_chips_registered_value)).setText(getResources()
                .getString(R.string.station_chips_registered_n, MainApp.mStation.getTeamsPunched()));
        ((TextView) findViewById(R.id.station_last_chip_time)).setText(
                Records.printTime(MainApp.mStation.getLastPunchTime(), "dd.MM.yyyy  HH:mm:ss"));
        // Show status block
        findViewById(R.id.station_status).setVisibility(View.VISIBLE);
    }

    /**
     * Callback to be invoked when an item in points list has been selected.
     */
    private final class PointSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
                                   final long rowId) {
            // Get point number at this position
            final int pointNumber =
                    MainApp.mDistance.getNumberFromPosition(parent.getSelectedItemPosition());
            // Compute mode for this point
            // Use station mode for current point and default mode for all other points
            final int pointMode;
            if (MainApp.mStation != null && pointNumber == MainApp.mStation.getNumber()) {
                pointMode = MainApp.mStation.getMode();
            } else {
                pointMode = getPointDefaultMode(pointNumber);
            }
            // Select new mode in modes list
            if (pointMode >= 0) {
                ((Spinner) findViewById(R.id.station_mode_spinner)).setSelection(pointMode);
            }
        }

        /**
         * Get default mode for selected point.
         *
         * @param pointNumber Point number in downloaded distance
         * @return One of Station.MODE_* constants or -1 in case of error
         */
        private int getPointDefaultMode(final int pointNumber) {
            return switch (MainApp.mDistance.getPointType(pointNumber)) {
                case -1 -> -1;
                case 0 -> StationAPI.MODE_INIT_CHIPS;
                case 1 -> StationAPI.MODE_OTHER_POINT;
                default -> StationAPI.MODE_FINISH_POINT;
            };
        }

        @Override
        public void onNothingSelected(final AdapterView<?> parent) {
            // do nothing
        }
    }
}
