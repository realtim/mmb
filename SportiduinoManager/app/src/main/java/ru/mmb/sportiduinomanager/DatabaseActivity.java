package ru.mmb.sportiduinomanager;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.Group;
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

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import ru.mmb.sportiduinomanager.model.Database;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.SiteRequest;

/**
 * Provides interaction with database at http://mmb.progressor.ru site.
 */
public final class DatabaseActivity extends MenuActivity {
    /**
     * Main application thread with persistent data.
     */
    private MainApp mAppContext;
    /**
     * True when download/upload async task is active.
     */
    private boolean mTransferActive;

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

    /**
     * Calculate MD5 from user password string.
     *
     * @param str String with a password
     * @return MD5 of the string
     */
    private static String md5(final String str) {
        try {
            // Create MD5 Hash
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(str.getBytes(Charset.forName("UTF-8")));
            final byte[] messageDigest = digest.digest();

            // Create Hex String
            final StringBuilder hexString = new StringBuilder();
            for (final byte aMessageDigest : messageDigest) {
                final String hexNumber = Integer.toHexString(0xFF & aMessageDigest);
                if (hexNumber.length() < 2) {
                    hexString.append('0');
                }
                hexString.append(hexNumber);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    @Override
    protected void onCreate(final Bundle instanceState) {
        super.onCreate(instanceState);
        mTransferActive = false;
        mAppContext = (MainApp) getApplicationContext();
        setContentView(R.layout.activity_database);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set selection in drawer menu to current mode
        getMenuItem(R.id.database).setChecked(true);
        updateMenuItems(R.id.database);
        // Update layout elements
        updateLayout();
    }

    /**
     * Update UL/DL buttons state and distance download block.
     *
     * @param distancePresent True if a valid distance is present in local db
     */
    private void updateButtons(final boolean distancePresent) {
        final LinearLayout dlDistance = findViewById(R.id.download_distance_layout);
        final Button sendRecordsButton = findViewById(R.id.send_results);
        final Button getResultsButton = findViewById(R.id.get_results);
        final Button sendDbButton = findViewById(R.id.send_database);
        if (distancePresent) {
            // Don't allow to reload database if it can contain important data
            if (MainApp.mDistance.canBeReloaded()) {
                dlDistance.setVisibility(View.VISIBLE);
                // set user email and test db flag from local database
                ((EditText) findViewById(R.id.user_email)).setText(mAppContext.getUserEmail());
                ((SwitchCompat) findViewById(R.id.test_database))
                        .setChecked(mAppContext.getTestSite() == 1);
            } else {
                dlDistance.setVisibility(View.GONE);
            }
            sendRecordsButton.setVisibility(View.VISIBLE);
            // Check if we have some unsent records
            if (MainApp.mAllRecords.hasUnsentRecords()) {
                sendRecordsButton.setAlpha(MainApp.ENABLED_BUTTON);
                sendRecordsButton.setClickable(true);
            } else {
                sendRecordsButton.setAlpha(MainApp.DISABLED_BUTTON);
                sendRecordsButton.setClickable(false);
            }
            // Always allow to download results from site and upload database
            getResultsButton.setVisibility(View.VISIBLE);
            sendDbButton.setVisibility(View.VISIBLE);
            // TODO: Enable 'Download results' button after download implementation
            getResultsButton.setAlpha(MainApp.DISABLED_BUTTON);
            getResultsButton.setClickable(false);
        } else {
            sendRecordsButton.setVisibility(View.GONE);
            getResultsButton.setVisibility(View.GONE);
            dlDistance.setVisibility(View.VISIBLE);
            sendDbButton.setVisibility(View.GONE);
        }
    }

    /**
     * Update Database activity layout
     * after start or distance download/results upload.
     */
    private void updateLayout() {
        int dbStatus;
        // Find database status
        if (MainApp.mDatabase == null) {
            dbStatus = Database.DB_STATE_FAILED;
        } else {
            dbStatus = MainApp.mDatabase.getDbStatus();
            if (dbStatus == Database.DB_STATE_OK
                    && (MainApp.mDistance.hasErrors() || MainApp.mTeams.hasErrors())) {
                dbStatus = Database.DB_STATE_DAMAGED;
            }
        }
        // Hide progress bar and update database status string
        final TextView statusMessage = findViewById(R.id.database_status_description);
        if (mTransferActive) {
            statusMessage.setVisibility(View.INVISIBLE);
            findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.database_status_progress).setVisibility(View.INVISIBLE);
            int statusColor;
            if (dbStatus == Database.DB_STATE_EMPTY || dbStatus == Database.DB_STATE_OK) {
                statusColor = R.color.text_primary;
            } else {
                statusColor = R.color.bg_secondary;
            }
            statusMessage.setTextColor(ResourcesCompat.getColor(getResources(), statusColor, getTheme()));
            statusMessage.setText(getStatusMessage(dbStatus));
            statusMessage.setVisibility(View.VISIBLE);
        }
        // Detect what we will show or hide
        final MenuItem databaseItem = getMenuItem(R.id.database);
        final Group dbContent = findViewById(R.id.database_content_group);
        switch (dbStatus) {
            case Database.DB_STATE_FAILED:
            case Database.DB_STATE_EMPTY:
            case Database.DB_STATE_DAMAGED:
                // Database is empty/broken/damaged, need to download it from server
                databaseItem.setTitle(getResources().getText(R.string.mode_cloud_download));
                databaseItem.setIcon(R.drawable.ic_cloud_download);
                // Update buttons state
                updateButtons(false);
                // Hide distance description
                dbContent.setVisibility(View.GONE);
                break;
            case Database.DB_STATE_OK:
                // Update buttons state
                updateButtons(true);
                // Set distance description
                String siteName;
                if (MainApp.mDistance.getTestSite() == 0) {
                    siteName = (String) getResources().getText(R.string.site_name_main);
                } else {
                    siteName = (String) getResources().getText(R.string.site_name_test);
                }
                ((TextView) findViewById(R.id.distance_version)).setText(getResources()
                        .getString(R.string.database_distance_version, siteName,
                                Records.printTime(MainApp.mDistance.getTimeDownloaded(), "dd.MM.yyyy HH:mm")));
                ((TextView) findViewById(R.id.distance_name)).setText(MainApp.mDistance.getRaidName());
                // Set Sportiduino records statistic
                final List<Integer> statistic = MainApp.mAllRecords.getStatistic();
                ((TextView) findViewById(R.id.database_local_init)).setText(getResources()
                        .getString(R.string.database_local_init, statistic.get(0),
                                statistic.get(1)));
                ((TextView) findViewById(R.id.database_local_results)).setText(getResources()
                        .getString(R.string.database_local_results, statistic.get(2),
                                statistic.get(3)));
                // Show database content
                dbContent.setVisibility(View.VISIBLE);
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
        // Check if we have another transfer waiting
        if (mTransferActive) {
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
        userPassword = md5(userPassword);
        // get download url
        int testSite;
        if (((SwitchCompat) findViewById(R.id.test_database)).isChecked()) {
            testSite = 1;
        } else {
            testSite = 0;
        }
        // Save email/password/site in main application
        // (as this activity can be recreated loosing these value)
        mAppContext.setAuthorizationParameters(sUserEmail, userPassword, testSite);
        // Clean password field to require to enter it again for next distance download
        etUserPassword.setText("");
        // Hide virtual keyboard
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Show progress bar instead of status text
        mTransferActive = true;
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        // start download
        final String chipInitName =
                getApplication().getResources().getString(R.string.init_point_name);
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(sUserEmail).userPassword(userPassword)
                        .testSite(testSite).chipInitName(chipInitName)
                        .database(MainApp.mDatabase)
                        .type(SiteRequest.TYPE_DL_DISTANCE).build();
        new AsyncSiteRequest(this).execute(siteRequest);
    }

    /**
     * Start upload of new Sportiduino records to site.
     *
     * @param view View of button clicked (unused)
     */
    public void startRecordsUpload(@SuppressWarnings("unused") final View view) {
        // Check if we have another transfer waiting
        if (mTransferActive) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_db_download_waiting),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Show progress bar instead of status text
        mTransferActive = true;
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        // Start upload
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(mAppContext.getUserEmail())
                        .userPassword(mAppContext.getUserPassword())
                        .testSite(mAppContext.getTestSite())
                        .database(MainApp.mDatabase)
                        .records(MainApp.mAllRecords)
                        .type(SiteRequest.TYPE_UL_CHIPS).build();
        new AsyncSiteRequest(this).execute(siteRequest);
    }

    /**
     * Start download of new results from site.
     *
     * @param view View of button clicked (unused)
     */
    public void startResultsDownload(@SuppressWarnings("unused") final View view) {
        // Check if we have another transfer waiting
        if (mTransferActive) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_db_download_waiting),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Show progress bar instead of status text
        mTransferActive = true;
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        // Start download
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(mAppContext.getUserEmail())
                        .userPassword(mAppContext.getUserPassword())
                        .testSite(mAppContext.getTestSite())
                        .database(MainApp.mDatabase)
                        .type(SiteRequest.TYPE_DL_RESULTS).build();
        new AsyncSiteRequest(this).execute(siteRequest);
    }

    /**
     * Start upload of local db for testing purposes.
     *
     * @param view View of button clicked (unused)
     */
    public void startDatabaseUpload(@SuppressWarnings("unused") final View view) {
        // Check if we have another transfer waiting
        if (mTransferActive) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.err_db_download_waiting),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Show progress bar instead of status text
        mTransferActive = true;
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        // Start download
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(mAppContext.getUserEmail())
                        .userPassword(mAppContext.getUserPassword())
                        .testSite(mAppContext.getTestSite())
                        .database(MainApp.mDatabase)
                        .type(SiteRequest.TYPE_UL_DATABASE).build();
        new AsyncSiteRequest(this).execute(siteRequest);
    }

    /**
     * Separate thread for async parsing of downloaded file with a distance.
     */
    private static final class AsyncSiteRequest extends AsyncTask<SiteRequest, Void, Integer> {
        /**
         * Reference to parent activity (which can cease to exist in any moment).
         */
        private final WeakReference<DatabaseActivity> mActivityRef;
        /**
         * Custom string which cannot be loaded from resources.
         */
        private String mCustomError;

        /**
         * Retain only a weak reference to the activity.
         *
         * @param context Calling activity context
         */
        private AsyncSiteRequest(final DatabaseActivity context) {
            super();
            mActivityRef = new WeakReference<>(context);
        }

        /**
         * Process server response and save distance and teams to SQLite database.
         *
         * @param request Previously prepared request to the site
         * @return True if succeeded
         */
        protected Integer doInBackground(final SiteRequest... request) {
            SiteRequest.RequestResult result;
            try {
                result = request[0].makeRequest();
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                result = SiteRequest.RequestResult.PARSE_ERROR;
            }
            switch (result) {
                case READ_ERROR:
                    return R.string.err_db_reading_response;
                case PARSE_ERROR:
                    return R.string.err_db_bad_response;
                case DATA_CHANGED:
                    // New records has been added during upload
                    // Force reload of records from local database
                    // and forget about sending some records to site
                    MainApp.setAllRecords(MainApp.mDatabase.loadRecords(), true);
                    return R.string.send_results_failure;
                case CUSTOM_ERROR:
                    mCustomError = request[0].getCustomError();
                    return -1;
                case OK:
                    switch (request[0].getRequestType()) {
                        case SiteRequest.TYPE_DL_DISTANCE:
                            // Copy loaded distance and teams to persistent memory
                            MainApp.setDistance(request[0].getDistance());
                            MainApp.setTeams(request[0].getTeams());
                            // Recreate list of records from local database
                            MainApp.setAllRecords(MainApp.mDatabase.loadRecords(), true);
                            return R.string.download_distance_success;
                        case SiteRequest.TYPE_UL_CHIPS:
                            // Update records in application memory
                            MainApp.setAllRecords(request[0].getRecords(), false);
                            return R.string.send_results_success;
                        case SiteRequest.TYPE_DL_RESULTS:
                            return R.string.unknown;
                        case SiteRequest.TYPE_UL_DATABASE:
                            return R.string.send_database_success;
                        default:
                            return R.string.err_internal_error;
                    }
                default:
                    return R.string.err_internal_error;
            }
        }

        /**
         * Show parsing result, delete the file and update screen layout.
         *
         * @param message False if connection attempt failed
         */
        protected void onPostExecute(final Integer message) {
            // Get a reference to the activity if it is still there
            final DatabaseActivity activity = mActivityRef.get();
            if (activity == null || activity.isFinishing()) return;
            // Show parsing result
            if (mCustomError == null) {
                Toast.makeText(activity.mAppContext, message, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity.mAppContext, mCustomError, Toast.LENGTH_LONG).show();
            }
            // Update activity layout
            activity.mTransferActive = false;
            activity.updateLayout();
            activity.updateMenuItems(R.id.database);
        }
    }
}
