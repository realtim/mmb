package ru.mmb.sportiduinomanager;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraHttpSender;
import org.acra.annotation.AcraToast;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;

import ru.mmb.sportiduinomanager.model.Database;
import ru.mmb.sportiduinomanager.model.Distance;
import ru.mmb.sportiduinomanager.model.Records;
import ru.mmb.sportiduinomanager.model.Station;
import ru.mmb.sportiduinomanager.model.Teams;

/**
 * Keeps all persistent activity data for application lifetime.
 */

// ACRA exception reporting system
@AcraCore(buildConfigClass = BuildConfig.class,
        reportFormat = StringFormat.JSON,
        reportContent = {org.acra.ReportField.REPORT_ID,
                org.acra.ReportField.APP_VERSION_CODE,
                org.acra.ReportField.APP_VERSION_NAME,
                org.acra.ReportField.PACKAGE_NAME,
                org.acra.ReportField.FILE_PATH,
                org.acra.ReportField.PHONE_MODEL,
                org.acra.ReportField.BRAND,
                org.acra.ReportField.PRODUCT,
                org.acra.ReportField.ANDROID_VERSION,
                org.acra.ReportField.BUILD,
                org.acra.ReportField.TOTAL_MEM_SIZE,
                org.acra.ReportField.AVAILABLE_MEM_SIZE,
                org.acra.ReportField.CUSTOM_DATA,
                org.acra.ReportField.STACK_TRACE,
                org.acra.ReportField.INITIAL_CONFIGURATION,
                org.acra.ReportField.CRASH_CONFIGURATION,
                org.acra.ReportField.DISPLAY,
                org.acra.ReportField.USER_APP_START_DATE,
                org.acra.ReportField.USER_CRASH_DATE,
                org.acra.ReportField.LOGCAT,
                org.acra.ReportField.EVENTSLOG,
                org.acra.ReportField.RADIOLOG,
                org.acra.ReportField.INSTALLATION_ID,
                org.acra.ReportField.DEVICE_FEATURES,
                org.acra.ReportField.ENVIRONMENT,
                org.acra.ReportField.SETTINGS_SYSTEM,
                org.acra.ReportField.SETTINGS_SECURE,
                org.acra.ReportField.SETTINGS_GLOBAL,
                org.acra.ReportField.THREAD_DETAILS,
                org.acra.ReportField.BUILD_CONFIG})
@AcraHttpSender(uri = "http://mmb.progressor.ru/php/mmbscripts/acra.php",
        httpMethod = HttpSender.Method.POST)
@AcraToast(resText = R.string.acra_toast_text)

public final class MainApp extends Application {
    /**
     * Alpha for disabled buttons appearance in the application.
     */
    public static final float DISABLED_BUTTON = .5f;
    /**
     * Alpha for enabled buttons appearance in the application.
     */
    public static final float ENABLED_BUTTON = 1f;
    /**
     * Distance downloaded from site or loaded from local database.
     */
    public static Distance mDistance = new Distance();
    /**
     * Teams with members downloaded from site or loaded from local database.
     */
    public static Teams mTeams = new Teams(0);
    /**
     * Database object for loading/saving data to local SQLite database.
     */
    public static Database mDatabase;
    /**
     * Connected Bluetooth station.
     */
    public static Station mStation;
    /**
     * List of all Sportiduino records received from connected stations.
     */
    public static Records mAllRecords = new Records(0);
    /**
     * Filtered list of records with team punches at connected station.
     * Last punch per team only. Should be equal to records in station flash memory.
     */
    public static Records mPointPunches = new Records(0);
    /**
     * Description of error occurred during application startup (if any).
     */
    private String mStartupError = "";
    /**
     * Email of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserEmail = "";
    /**
     * Password of the user who authorized (or tried to authorize) at the site.
     */
    private String mUserPassword = "";
    /**
     * Which website database we are using, main of test.
     */
    private int mTestSite;
    /**
     * List of previously discovered Bluetooth devices.
     */
    private List<BluetoothDevice> mBTDeviceList = new ArrayList<>();
    /**
     * Current team number at chip initialization.
     */
    private int mTeamNumber;

    /**
     * Current members selection mask at chip initialization / active point.
     */
    private int mTeamMask;

    /**
     * Current position in team list at active point.
     */
    private int mTeamListPosition;

    /**
     * Save distance to persistent memory.
     *
     * @param distance The distance to save
     */
    public static void setDistance(final Distance distance) {
        mDistance = distance;
    }

    /**
     * Save database to persistent memory.
     *
     * @param database The database to save
     */
    private static void setDatabase(final Database database) {
        mDatabase = database;
    }

    /**
     * Save teams with members to persistent memory.
     *
     * @param teams The teams to save
     */
    public static void setTeams(final Teams teams) {
        mTeams = teams;
    }

    /**
     * Save the connected Bluetooth station.
     *
     * @param station Bluetooth device
     */
    public static void setStation(final Station station) {
        mStation = station;
    }

    /**
     * Save list of all Sportiduino records to application memory.
     *
     * @param records Records list to save
     * @param force   Force replacing of old records with new
     */
    // TODO: remove 'force' parameter
    public static void setAllRecords(final Records records, final boolean force) {
        if (force || mAllRecords == null) {
            // Forget old records and replace them with new
            mAllRecords = records;
            return;
        }
        // Check if old list has more records then new
        if (mAllRecords.size() > records.size() && mDatabase != null) {
            // Reload records from local database
            // (it is better to lose some records statuses then the whole records)
            mAllRecords = mDatabase.loadRecords();
        } else {
            mAllRecords = records;
        }
    }

    /**
     * Save list of team punches at connected station to application memory.
     *
     * @param records Records list to save
     */
    public static void setPointPunches(final Records records) {
        mPointPunches = records;
    }

    /**
     * Get startup error message (if any).
     *
     * @return Error message string
     */
    public String getStartupError() {
        return mStartupError;
    }

    /**
     * Get the email of the user who authorized (or tried to authorize) at the site.
     *
     * @return User email
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get the password of the user who authorized (or tried to do it) at the site.
     *
     * @return User password
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return True if we are using test database
     */
    public int getTestSite() {
        return mTestSite;
    }

    /**
     * Keep email and password of authorized user
     * and info about which website database we are using, main of test.
     *
     * @param userEmail    Email entered by user
     * @param userPassword Password entered by user
     * @param testSite     Test site selection (equal 1 if selected)
     */
    public void setAuthorizationParameters(final String userEmail, final String userPassword, final int testSite) {
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
    }

    /**
     * Get the list of previously discovered Bluetooth devices.
     *
     * @return List od Bluetooth devices
     */
    public List<BluetoothDevice> getBTDeviceList() {
        return mBTDeviceList;
    }

    /**
     * Save the list of previously discovered Bluetooth devices.
     *
     * @param deviceList List od Bluetooth devices
     */
    public void setBTDeviceList(final List<BluetoothDevice> deviceList) {
        mBTDeviceList = deviceList;
    }

    /**
     * Get the current team number during chip initialization.
     *
     * @return Team number
     */
    public int getTeamNumber() {
        return mTeamNumber;
    }

    /**
     * Save the current team number during chip initialization.
     *
     * @param teamNumber Team number
     */
    public void setTeamNumber(final int teamNumber) {
        mTeamNumber = teamNumber;
    }

    /**
     * Get the current team mask at chip initialization / active point.
     *
     * @return Team mask
     */
    public int getTeamMask() {
        return mTeamMask;
    }

    /**
     * Save the current team mask at chip initialization / active point.
     *
     * @param teamMask Team mask
     */
    public void setTeamMask(final int teamMask) {
        mTeamMask = teamMask;
    }

    /**
     * Get the current position in team list.
     *
     * @return Zero-based position
     */
    public int getTeamListPosition() {
        return mTeamListPosition;
    }

    /**
     * Save the current position in team list.
     *
     * @param teamListPosition Zero-based position
     */
    public void setTeamListPosition(final int teamListPosition) {
        mTeamListPosition =
                teamListPosition;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        final Context context = getApplicationContext();
        // Set russian locale for debug build type
        if (BuildConfig.DEBUG) {
            switchToRussian(context);
        }
        // Try to open/create local SQLite database
        try {
            setDatabase(new Database(context));
        } catch (IOException e) {
            mStartupError = context.getString(R.string.err_db_cant_create_dir).concat(e.getMessage());
            return;
        } catch (SQLiteException e) {
            mStartupError = e.getMessage();
            return;
        }
        // Try to load distance amd teams from database if it is not empty
        if (mDatabase.getDbStatus() == Database.DB_STATE_OK) {
            try {
                final Distance distance =
                        mDatabase.loadDistance(context.getString(R.string.mode_chip_init));
                if (distance != null && !distance.hasErrors()) {
                    setDistance(distance);
                    // Get user email, password and test/main database flag from loaded distance
                    mUserEmail = mDistance.getUserEmail();
                    mUserPassword = mDistance.getUserPassword();
                    mTestSite = mDistance.getTestSite();
                }
                final Teams teams = mDatabase.loadTeams();
                if (teams != null && !teams.hasErrors()) {
                    setTeams(teams);
                }
            } catch (SQLiteException e) {
                mStartupError = e.getMessage();
            }
        }
        // Try to load records from database
        if (mDatabase.getDbStatus() == Database.DB_STATE_OK
                || mDatabase.getDbStatus() == Database.DB_STATE_EMPTY) {
            try {
                setAllRecords(mDatabase.loadRecords(), true);
            } catch (SQLiteException e) {
                mStartupError = e.getMessage();
            }
        }
    }

    /**
     * Switch to russian locale in debug version.
     *
     * @param context Application context
     */
    private void switchToRussian(final Context context) {
        final Locale locale = new Locale("ru");
        Locale.setDefault(locale);
        final Resources res = context.getResources();
        final Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @Override
    protected void attachBaseContext(final Context base) {
        super.attachBaseContext(base);
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
