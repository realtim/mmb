package ru.mmb.sportiduinomanager;

import android.content.Context;
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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     * Copy of activity context for AsyncTask.
     */
    private DatabaseActivity mContext;
    /**
     * True when download/upload async task is active.
     */
    private boolean mDownloadActive;

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
            digest.update(str.getBytes(StandardCharsets.UTF_8));
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
        mContext = this;
        mDownloadActive = false;
        mMainApplication = (MainApplication) this.getApplication();
        mDistance = mMainApplication.getDistance();
        mTeams = mMainApplication.getTeams();
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
        if (mDownloadActive) {
            statusMessage.setVisibility(View.INVISIBLE);
            findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.database_status_progress).setVisibility(View.INVISIBLE);
            statusMessage.setVisibility(View.VISIBLE);
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
        if (mDownloadActive) {
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
        mMainApplication.setAuthorizationParameters(sUserEmail, userPassword, testSite);

        // Clean password field to require to enter it again for next distance download
        etUserPassword.setText("");
        // Hide virtual keyboard
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        // Show progress bar instead of status text
        mDownloadActive = true;
        findViewById(R.id.database_status_description).setVisibility(View.INVISIBLE);
        findViewById(R.id.database_status_progress).setVisibility(View.VISIBLE);

        // start download
        final String chipInitName =
                mMainApplication.getContext().getResources().getString(R.string.mode_chip_init);
        final SiteRequest siteRequest =
                SiteRequest.builder().userEmail(sUserEmail).userPassword(userPassword)
                        .testSite(testSite).chipInitName(chipInitName)
                        .database(mMainApplication.getDatabase())
                        .type(SiteRequest.TYPE_DL_DISTANCE).build();
        new AsyncSiteRequest(mContext).execute(siteRequest);
    }

    /**
     * Separate thread for async parsing of downloaded file with a distance.
     */
    private static class AsyncSiteRequest extends AsyncTask<SiteRequest, Void, Integer> {
        /**
         * Reference to parent activity (which can cease to exist in any moment).
         */
        private final WeakReference<DatabaseActivity> mActivityRef;
        /**
         * Reference to main application thread.
         */
        private final MainApplication mMainApplication;
        /**
         * Custom string which cannot be loaded from resources.
         */
        private String mCustomError;

        /**
         * Retain only a weak reference to the activity.
         *
         * @param context Calling activity context
         */
        AsyncSiteRequest(final DatabaseActivity context) {
            super();
            mActivityRef = new WeakReference<>(context);
            mMainApplication = (MainApplication) context.getApplication();
        }

        /**
         * Process server response and save distance and teams to SQLite database.
         *
         * @param request Previously prepared request to the site
         * @return True if succeeded
         */
        protected Integer doInBackground(final SiteRequest... request) {
            int result;
            try {
                result = request[0].makeRequest();
            } catch (IOException e) {
                result = SiteRequest.LOAD_READ_ERROR;

            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                result = SiteRequest.LOAD_PARSE_ERROR;
            }
            switch (result) {
                case SiteRequest.LOAD_READ_ERROR:
                    return R.string.err_db_reading_response;
                case SiteRequest.LOAD_PARSE_ERROR:
                    return R.string.err_db_bad_response;
                case SiteRequest.LOAD_FATAL_ERROR:
                    return R.string.err_db_internal_error;
                case SiteRequest.LOAD_CUSTOM_ERROR:
                    mCustomError = request[0].getCustomError();
                    return -1;
                case SiteRequest.LOAD_OK:
                    // Copy loaded distance and teams to persistent memory
                    mMainApplication.setDistance(request[0].getDistance());
                    mMainApplication.setTeams(request[0].getTeams());
                    return R.string.download_distance_success;
                default:
                    return R.string.unknown;
            }
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
            // Get a reference to the activity if it is still there
            final DatabaseActivity activity = mActivityRef.get();
            if (activity == null || activity.isFinishing()) return;
            // Update distance and teams class members
            activity.mDistance = mMainApplication.getDistance();
            activity.mTeams = mMainApplication.getTeams();
            // Update activity layout
            activity.mDownloadActive = false;
            activity.updateLayout();
        }

    }

}
