package ru.mmb.sportiduinomanager;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

import ru.mmb.sportiduinomanager.model.Database;
import ru.mmb.sportiduinomanager.model.StationAPI;

/**
 * Provides left menu via NavigationDrawer.
 */
public class MenuActivity extends AppCompatActivity {
    /**
     * Sliding left menu view.
     */
    private NavigationView mNavigationView;
    /**
     * Parent Drawer layout with toolbar and menu.
     */
    private DrawerLayout mDrawerLayout;

    @SuppressLint("NonConstantResourceId")
    @Override
    public final void setContentView(@LayoutRes final int layoutResID) {
        // This is going to be our actual root layout.
        @SuppressLint("InflateParams") final DrawerLayout fullLayout =
                (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_main, null);
        // inflate the child's view
        final FrameLayout activityContainer = fullLayout.findViewById(R.id.activity_content);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        // Note that we don't pass the child's layoutId to the parent,
        // instead we pass it our inflated layout
        super.setContentView(fullLayout);

        // Add Toolbar to Main screen
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create Navigation drawer and inflate layout
        mDrawerLayout = findViewById(R.id.drawer_layout);
        // Set behavior of Navigation drawer
        mNavigationView = findViewById(R.id.navigation_view);
        // This method will trigger on item Click of navigation menu
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            // Process selected menu item
            Intent activity = null;
            switch (menuItem.getItemId()) {
                case R.id.database:
                    activity = new Intent(getApplicationContext(), DatabaseActivity.class);
                    break;
                case R.id.bluetooth:
                    activity = new Intent(getApplicationContext(), BluetoothActivity.class);
                    break;
                case R.id.chip_init:
                    activity = new Intent(getApplicationContext(), ChipInitActivity.class);
                    break;
                case R.id.control_point:
                    activity = new Intent(getApplicationContext(), ControlPointActivity.class);
                    break;
                case R.id.team_list:
                    Toast.makeText(this, R.string.err_todo_team_list,
                            Toast.LENGTH_LONG).show();
                    break;
                case R.id.chip_info:
                    activity = new Intent(getApplicationContext(), ChipInfoActivity.class);
                    break;
                default:
            }
            // Switch to new activity
            if (activity != null) {
                activity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(activity);
            }
            // Closing drawer on item click
            mDrawerLayout.closeDrawers();
            return true;
        });
    }

    /**
     * Set russian locale for all activities for debug build type.
     *
     * @param base Activity base context
     */
    @Override
    protected void attachBaseContext(final Context base) {
        if (BuildConfig.DEBUG) {
            super.attachBaseContext(MainApp.switchToRussian(base));
        } else {
            super.attachBaseContext(base);
        }
    }

    /**
     * Method called at the start of activity.
     *
     * @param instanceState Activity instance
     */
    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        // Show startup screen, overload this method to show another view
        setContentView(R.layout.startup_screen);
        final String html = getResources().getString(R.string.app_usage);
        ((WebView) findViewById(R.id.startup_message)).loadDataWithBaseURL(null, html, "text/html",
                "utf-8", null);
        // Update menu items
        updateMenuItems(0);
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Get access from child activity to the item of menu slider.
     *
     * @param itemId Menu item id
     * @return Menu item handler
     */
    MenuItem getMenuItem(final int itemId) {
        return mNavigationView.getMenu().findItem(itemId);
    }

    /**
     * Process click on back button in drawer header.
     *
     * @param view Unused
     */
    public void closeNavigationDrawer(@SuppressWarnings("unused") final View view) {
        mDrawerLayout.closeDrawers();
    }

    /**
     * Update menu titles and icons according to app state.
     *
     * @param activeItem Currently selected menu item id or 0 for startup screen
     */
    public void updateMenuItems(final int activeItem) {
        // Get current data status
        final boolean readyForWork = MainApp.mStation != null && MainApp.mStation.connect()
                && MainApp.mDatabase != null && MainApp.mDistance.getTimeDownloaded() != 0;
        // Update 'Database' menu item
        final MenuItem databaseItem = mNavigationView.getMenu().findItem(R.id.database);
        final int dbStatus;
        if (MainApp.mDatabase == null) {
            dbStatus = Database.DB_STATE_FAILED;
        } else {
            dbStatus = MainApp.mDatabase.getDbStatus();
        }
        if (dbStatus == Database.DB_STATE_OK) {
            if (MainApp.mAllRecords.hasUnsentRecords()) {
                final SpannableString title = new SpannableString(getResources().getText(R.string.mode_cloud_upload));
                title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.bg_secondary)), 0,
                        title.length(), 0);
                databaseItem.setTitle(title);
                databaseItem.setIcon(R.drawable.ic_cloud_upload);
            } else {
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_done));
                databaseItem.setIcon(R.drawable.ic_cloud_done);
            }
        } else {
            databaseItem.setTitle(getResources().getText(R.string.mode_cloud_download));
            databaseItem.setIcon(R.drawable.ic_cloud_download);
        }
        // Update 'Bluetooth' menu item
        final MenuItem bluetoothItem = mNavigationView.getMenu().findItem(R.id.bluetooth);
        if (MainApp.mStation == null) {
            bluetoothItem.setTitle(getResources().getText(R.string.mode_bluetooth_select));
        } else {
            bluetoothItem.setTitle(getResources().getString(R.string.mode_bluetooth_set,
                    MainApp.mStation.getName()));
        }
        bluetoothItem.setEnabled(MainApp.mDatabase != null && MainApp.mDistance.getTimeDownloaded() != 0);
        // Get the name of the point which is selected in connected station
        String pointName = "";
        if (MainApp.mStation != null) {
            pointName = MainApp.mDistance.getPointName(MainApp.mStation.getNumber(),
                    getResources().getString(R.string.control_point_prefix));
        }
        // Update 'Chip Init' menu item
        final MenuItem chipInitItem = mNavigationView.getMenu().findItem(R.id.chip_init);
        if (readyForWork && MainApp.mStation.getMode() == StationAPI.MODE_INIT_CHIPS) {
            chipInitItem.setEnabled(true);
            if (MainApp.mStation.getNumber() == 0) {
                chipInitItem.setTitle(getResources().getText(R.string.mode_chip_init));
            } else {
                chipInitItem.setTitle(getResources().getString(R.string.mode_chip_init_name,
                        pointName));
            }
        } else {
            chipInitItem.setEnabled(false);
            chipInitItem.setTitle(getResources().getText(R.string.mode_chip_init));
        }
        // Update 'Control Point' item
        final MenuItem controlPointItem = mNavigationView.getMenu().findItem(R.id.control_point);
        if (readyForWork && MainApp.mStation.getMode() != StationAPI.MODE_INIT_CHIPS) {
            controlPointItem.setEnabled(true);
            controlPointItem.setTitle(getResources().getString(R.string.mode_control_point_name,
                    pointName));
        } else {
            controlPointItem.setEnabled(false);
            controlPointItem.setTitle(getResources().getText(R.string.mode_control_point));
        }
        // 'View results' is not implemented yet
        mNavigationView.getMenu().findItem(R.id.team_list).setEnabled(false);
        // Update 'Chip Info' menu item
        final MenuItem chipInfoItem = mNavigationView.getMenu().findItem(R.id.chip_info);
        chipInfoItem.setEnabled(readyForWork && MainApp.mStation.getMode() == StationAPI.MODE_INIT_CHIPS);
        // Update toolbar title
        if (activeItem != 0) {
            final String title = Objects.requireNonNull(mNavigationView.getMenu().findItem(activeItem)
                    .getTitle()).toString();
            Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        }
        if (MainApp.mStation != null) {
            // Start/stop station monitoring service after selecting new activity
            if (activeItem == R.id.control_point) {
                startMonitoringService();
            } else {
                stopMonitoringService();
            }
        }
    }

    /**
     * Background thread for periodic querying of connected station.
     */
    void startMonitoringService() {
        MainApp.mStation.setQueryingAllowed(true);
        // Return if the service is already running
        final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (final ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (StationMonitorService.class.getName().equals(service.service.getClassName())) return;
        }
        // Start the service
        final Intent intent = new Intent(this, StationMonitorService.class);
        startService(intent);
    }

    /**
     * Stops rescheduling of periodic station query.
     */
    void stopMonitoringService() {
        MainApp.mStation.setQueryingAllowed(false);
        final Intent intent = new Intent(this, StationMonitorService.class);
        stopService(intent);
    }
}
