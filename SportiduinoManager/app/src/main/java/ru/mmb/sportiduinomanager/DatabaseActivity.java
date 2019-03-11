package ru.mmb.sportiduinomanager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import ru.mmb.sportiduinomanager.model.Database;
import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.SiteRequest;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Provides interaction with database at http://mmb.progressor.ru site.
 */
public final class DatabaseActivity extends MainActivity {
    /**
     * Local copy of distance (downloaded from site or loaded from local database).
     */
    private Distance mDistance;

    /**
     * Local copy of teams with members (from site or local database).
     */
    private Teams mTeams;

    /**
     * Main application thread with persistent data.
     */
    private MainApplication mMainApplication;
    /**
     * Async download thread manager.
     */
    private DownloadManager mDownloadManager;
    /**
     * Copy of activity context for AsyncTask.
     */
    private DatabaseActivity mContext;

    /**
     * Receiver of "download completed" events.
     */
    private final BroadcastReceiver mDistanceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // check if it was our download which have been completed
            final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
            String action = "";
            if (downloadId == mMainApplication.getDistanceDownloadId()) {
                action = "1";
            }
            if ("".equals(action)) return;
            // check download status
            final Cursor cursor = mDownloadManager.query(
                    new DownloadManager.Query().setFilterById(downloadId));
            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }
            if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    != DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(context, getResources().getString(R.string.err_db_download_failed)
                                + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)),
                        Toast.LENGTH_LONG).show();
                cursor.close();
                return;
            }
            final String path = Uri.parse(cursor
                    .getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))).getPath();
            cursor.close();
            // Parse the file and load it to database in background thread
            new ProcessFile(mContext).execute(path, action);
        }
    };

    /**
     * Get descriptive string message about database status.
     *
     * @param status Status of the database
     * @return Resource id with string message about the status
     */
    private static int getStatusMessage(final int status) {
        switch (status) {
            case Database.DB_STATE_FAILED:
                return R.string.database_fatal_error;
            case Database.DB_STATE_EMPTY:
                return R.string.database_empty;
            case Database.DB_STATE_OK:
                return R.string.database_ok;
            case Database.DB_STATE_DAMAGED:
                return R.string.database_damaged;
            default:
                return R.string.database_status_unknown;
        }
    }

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mContext = this;
        mMainApplication = (MainApplication) this.getApplication();
        mDistance = mMainApplication.getDistance();
        mTeams = mMainApplication.getTeams();
        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        setContentView(R.layout.activity_database);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.database).setChecked(true);
        // TODO: Change toolbar title
        // Disable startup animation
        overridePendingTransition(0, 0);
        // Update layout elements
        updateLayout();
        // Register download receiver
        registerReceiver(mDistanceReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister download receiver
        unregisterReceiver(mDistanceReceiver);
    }

    /**
     * Update Database activity layout
     * after start or distance download/results upload.
     */
    private void updateLayout() {
        int dbStatus;
        // Get database from persistent memory
        final Database database = mMainApplication.getDatabase();
        // Find its status
        if (database == null) {
            dbStatus = Database.DB_STATE_FAILED;
        } else {
            dbStatus = database.getDbStatus();
            if (dbStatus == Database.DB_STATE_OK && (mDistance == null || mTeams == null)) {
                dbStatus = Database.DB_STATE_DAMAGED;
            }
        }
        // Hide progress bar and update database status string
        final TextView statusMessage = findViewById(R.id.database_status_description);
        int statusColor;
        if (dbStatus == Database.DB_STATE_EMPTY || dbStatus == Database.DB_STATE_OK) {
            statusColor = R.color.text_primary;
        } else {
            statusColor = R.color.bg_secondary;
        }
        statusMessage.setTextColor(ResourcesCompat.getColor(getResources(), statusColor, getTheme()));
        statusMessage.setText(getStatusMessage(dbStatus));
        if (mMainApplication.getDistanceDownloadId() == -1) {
            findViewById(R.id.database_status_progress).setVisibility(View.INVISIBLE);
            statusMessage.setVisibility(View.VISIBLE);
        } else {
            statusMessage.setVisibility(View.INVISIBLE);
            findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        }
        // Detect what we will show or hide
        final MenuItem databaseItem = getMenuItem(R.id.database);
        final Button getResultsButton = findViewById(R.id.get_results);
        final Button sendResultsButton = findViewById(R.id.send_results);
        final LinearLayout dlDistanceLayout = findViewById(R.id.download_distance_layout);
        final LinearLayout dbContentLayout = findViewById(R.id.database_content_layout);
        switch (dbStatus) {
            case Database.DB_STATE_FAILED:
                // Database is broken, can't do anything
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_download));
                databaseItem.setIcon(R.drawable.ic_cloud_download);
                getResultsButton.setVisibility(View.GONE);
                sendResultsButton.setVisibility(View.GONE);
                dlDistanceLayout.setVisibility(View.GONE);
                dbContentLayout.setVisibility(View.GONE);
                break;
            case Database.DB_STATE_EMPTY:
            case Database.DB_STATE_DAMAGED:
                // Database is empty or damaged, need to download it from server
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_download));
                databaseItem.setIcon(R.drawable.ic_cloud_download);
                getResultsButton.setVisibility(View.GONE);
                sendResultsButton.setVisibility(View.GONE);
                dlDistanceLayout.setVisibility(View.VISIBLE);
                dbContentLayout.setVisibility(View.GONE);
                break;
            case Database.DB_STATE_OK:
                // Don't allow to reload database if it contains important data
                if (mDistance.canBeReloaded()) {
                    dlDistanceLayout.setVisibility(View.VISIBLE);
                } else {
                    dlDistanceLayout.setVisibility(View.GONE);
                }
                // set user email and test db flag from local database
                ((EditText) findViewById(R.id.user_email)).setText(mMainApplication.getUserEmail());
                ((SwitchCompat) findViewById(R.id.test_database)).setChecked(mMainApplication.getTestSite() == 1);
                // TODO: add check for showing UL/DL buttons
                getResultsButton.setAlpha(MainApplication.DISABLED_BUTTON);
                getResultsButton.setClickable(false);
                getResultsButton.setVisibility(View.VISIBLE);
                sendResultsButton.setAlpha(MainApplication.DISABLED_BUTTON);
                sendResultsButton.setClickable(false);
                sendResultsButton.setVisibility(View.VISIBLE);
                // Show database content
                String siteName;
                if (mDistance.getTestSite() == 0) {
                    siteName = (String) getResources().getText(R.string.site_name_main);
                } else {
                    siteName = (String) getResources().getText(R.string.site_name_test);
                }
                ((TextView) findViewById(R.id.distance_version)).setText(getResources()
                        .getString(R.string.database_distance_version, siteName,
                                mDistance.getDownloadDate()));
                ((TextView) findViewById(R.id.distance_name)).setText(mDistance.getRaidName());
                dbContentLayout.setVisibility(View.VISIBLE);
                // Update main menu item
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_done));
                databaseItem.setIcon(R.drawable.ic_cloud_done);
                break;
            default:
        }
    }

    /**
     * Start download of last distance from site.
     *
     * @param view View of button clicked
     */
    public void startDistanceDownload(final View view) {
        // Check if we have another download waiting
        if (mMainApplication.getDistanceDownloadId() != -1) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_db_download_waiting),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // check for empty/bad values
        final EditText etUserEmail = findViewById(R.id.user_email);
        final String sUserEmail = etUserEmail.getText().toString();
        if (sUserEmail.isEmpty()) {
            etUserEmail.setError(getResources().getString(R.string.err_db_empty_email));
            return;
        }
        if (!sUserEmail.contains("@")) {
            etUserEmail.setError(getResources().getString(R.string.err_db_bad_email));
            return;
        }
        final EditText etUserPassword = findViewById(R.id.user_password);
        String userPassword = etUserPassword.getText().toString();
        if (userPassword.isEmpty()) {
            etUserPassword.setError(getResources().getString(R.string.err_db_empty_password));
            return;
        }
        userPassword = SiteRequest.md5(userPassword);

        // get download url
        int testSite;
        if (((SwitchCompat) findViewById(R.id.test_database)).isChecked()) {
            testSite = 1;
        } else {
            testSite = 0;
        }

        // Save email/password/site in main application
        // (as this activity can be recreated loosing these value)
        mMainApplication.setAuthorizationParameters(sUserEmail, userPassword, testSite);

        // Clean password field to require to enter it again for next distance download
        etUserPassword.setText("");
        // Hide virtual keyboard
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Show progress bar instead of status text
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);

        // start download
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(sUserEmail).userPassword(userPassword)
                        .testSite(testSite).title(getResources().getString(R.string.app_name))
                        .context(getApplicationContext()).downloadManager(mDownloadManager).build();
        mMainApplication.setDistanceDownloadId(siteRequest.askDistance());
    }

    /**
     * Separate thread for async parsing of downloaded file with a distance.
     */
    private static class ProcessFile extends AsyncTask<String, Void, Integer> {
        /**
         * Reference to parent activity (which can cease to exist in any moment).
         */
        private final WeakReference<DatabaseActivity> mActivityRef;
        /**
         * Reference to main application thread.
         */
        private final MainApplication mMainApplication;
        /**
         * Downloaded file with some data.
         */
        private File mFile;
        /**
         * Custom string which cannot be loaded from resources.
         */
        private String mCustomError;

        /**
         * Retain only a weak reference to the activity.
         *
         * @param context Calling activity context
         */
        ProcessFile(final DatabaseActivity context) {
            super();
            mActivityRef = new WeakReference<>(context);
            mMainApplication = (MainApplication) context.getApplication();
        }

        /**
         * Process server response and save distance and teams to SQLite database.
         *
         * @param path Path to file
         * @return True if succeeded
         */
        protected Integer doInBackground(final String... path) {
            // Save file to be able to delete it later
            mFile = new File(path[0]);
            final String action = path[1];
            if ("1".equals(action)) {
                // Load downloaded distance and teams into memory and local database
                final SiteRequest siteRequest = SiteRequest.builder()
                        .userEmail(mMainApplication.getUserEmail())
                        .userPassword(mMainApplication.getUserPassword())
                        .testSite(mMainApplication.getTestSite()).build();
                final int result = siteRequest.loadDistance(mFile,
                        mMainApplication.getContext().getResources()
                                .getString(R.string.mode_chip_init),
                        mMainApplication.getDatabase());
                switch (result) {
                    case SiteRequest.LOAD_READ_ERROR:
                        return R.string.err_db_reading_response;
                    case SiteRequest.LOAD_PARSE_ERROR:
                        return R.string.err_db_bad_response;
                    case SiteRequest.LOAD_CUSTOM_ERROR:
                        mCustomError = siteRequest.getCustomError();
                        return -1;
                    case SiteRequest.LOAD_OK:
                        // Copy loaded distance and teams to persistent memory
                        mMainApplication.setDistance(siteRequest.getDistance());
                        mMainApplication.setTeams(siteRequest.getTeams());
                        return R.string.download_distance_success;
                    default:
                        return R.string.unknown;
                }
            }
            return R.string.unknown;
        }

        /**
         * Show parsing result, delete the file and update screen layout.
         *
         * @param message False if connection attempt failed
         */
        protected void onPostExecute(final Integer message) {
            // Show parsing result
            if (mCustomError == null) {
                Toast.makeText(mMainApplication, message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mMainApplication, mCustomError, Toast.LENGTH_LONG).show();
            }
            // Delete downloaded file
            if (!mFile.delete()) {
                Toast.makeText(mMainApplication, R.string.err_db_reading_response,
                        Toast.LENGTH_LONG).show();
            }
            mMainApplication.setDistanceDownloadId(-1L);
            // Get a reference to the activity if it is still there
            final DatabaseActivity activity = mActivityRef.get();
            if (activity == null || activity.isFinishing()) return;
            // Update distance and teams class members
            activity.mDistance = mMainApplication.getDistance();
            activity.mTeams = mMainApplication.getTeams();
            // Update activity layout
            activity.updateLayout();
        }
    }

}
