package ru.mmb.sportiduinomanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import ru.mmb.sportiduinomanager.model.Distance;

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
    public void setContentView(@LayoutRes final int layoutResID) {
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
        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    // This method will trigger on item Click of navigation menu
                    @Override
                    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
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
                                Toast.makeText(MainActivity.this, "Chip Init", Toast.LENGTH_LONG).show();
                                break;
                            case R.id.active_point:
                                Toast.makeText(MainActivity.this, "Active Point", Toast.LENGTH_LONG).show();
                                break;
                            case R.id.team_list:
                                Toast.makeText(MainActivity.this, "Team List", Toast.LENGTH_LONG).show();
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
                    }
                });
    }

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        // Show startup screen, overload this method to show another view
        setContentView(R.layout.startup_screen);
        // Show error from application onCreate (if any)
        final MainApplication appState = (MainApplication) getApplication();
        final String startupError = appState.getStartupError();
        if (startupError != null) {
            final TextView message = findViewById(R.id.startup_message);
            message.setText(startupError);
        }
        final SQLiteDatabase database = appState.getDatabase();
        if (startupError == null && database == null) {
            final TextView message = findViewById(R.id.startup_message);
            message.setText(R.string.err_db_is_null);
        }
        // Update 'Database' menu item
        final MenuItem databaseItem = mNavigationView.getMenu().findItem(R.id.database);
        final int dbStatus = Distance.getDbStatus(database);
        if (dbStatus == Distance.DB_STATE_FAILED || dbStatus == Distance.DB_STATE_EMPTY) {
            databaseItem.setTitle(getResources().getText(R.string.mode_cloud_download));
            databaseItem.setIcon(R.drawable.ic_cloud_download);
        } else {
            databaseItem.setTitle(getResources().getText(R.string.mode_cloud_done));
            databaseItem.setIcon(R.drawable.ic_cloud_done);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        return super.onOptionsItemSelected(item);
    }

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
}