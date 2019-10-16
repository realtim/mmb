package ru.mmb.sportiduinomanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Objects;

import ru.mmb.sportiduinomanager.model.Database;
import ru.mmb.sportiduinomanager.model.StationAPI;

/**
 * Provides left menu via NavigationDrawer.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Sliding left menu view.
     */
    private NavigationView mNavigationView;
    /**
     * Parent Drawer layout with toolbar and menu.
     */
    private DrawerLayout mDrawerLayout;

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
                    //activity = new Intent(getApplicationContext(), ChipInfoActivity.class);
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
        // Show error from application onCreate (if any)
        final MainApp mainApplication = (MainApp) getApplication();
        final String startupError = mainApplication.getStartupError();
        if (!"".equals(startupError)) {
            Toast.makeText(this, startupError, Toast.LENGTH_LONG).show();
        }
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
        int dbStatus;
        if (MainApp.mDatabase == null) {
            dbStatus = Database.DB_STATE_FAILED;
        } else {
            dbStatus = MainApp.mDatabase.getDbStatus();
        }
        if (dbStatus == Database.DB_STATE_OK) {
            if (MainApp.mAllRecords.hasUnsentRecords()) {
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_upload));
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
        if (MainApp.mDatabase == null || MainApp.mDistance.getTimeDownloaded() == 0) {
            bluetoothItem.setEnabled(false);
        } else {
            bluetoothItem.setEnabled(true);
        }
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
        // Update 'Chip Info' menu item
        final MenuItem chipInfoItem = mNavigationView.getMenu().findItem(R.id.chip_info);
        if (readyForWork && MainApp.mStation.getMode() == StationAPI.MODE_INIT_CHIPS) {
            chipInfoItem.setEnabled(true);
        } else {
            chipInfoItem.setEnabled(false);
        }
        // Update toolbar title
        if (activeItem != 0) {
            Objects.requireNonNull(getSupportActionBar())
                    .setTitle(mNavigationView.getMenu().findItem(activeItem).getTitle());
        }
        // Stop station monitoring service if we are not on control point screen
        if (activeItem != R.id.control_point && MainApp.mStation != null) {
            MainApp.mStation.setQueryingAllowed(false);
            final Intent intent = new Intent(this, StationMonitorService.class);
            stopService(intent);
        }
    }
}
