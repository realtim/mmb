package ru.mmb.sportiduinomanager;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.widget.Toast;

import java.io.IOException;
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
import ru.mmb.sportiduinomanager.model.StationAPI;
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
    public static StationAPI mStation;
    /**
     * List of all Sportiduino records received from connected stations.
     */
    public static Records mAllRecords = new Records(0);
    /**
     * List of punches read from a chip at ChipInfo activity.
     */
    public static Records mChipPunches = new Records(0);
    /**
     * Distance downloaded from site or loaded from local database.
     */
    public static Distance mDistance = new Distance();
    /**
     * Filtered list of records with team punches at connected station.
     * Last punch per team only. Should be equal to records in station flash memory.
     */
    static Records mPointPunches = new Records(0);
    /**
     * Current state of all UI elements to restore after activity recreation.
     */
    static UIState mUIState = new UIState();
    /**
     * True if ControlPointActivity is running in foreground.
     */
    private static boolean mCPActivityActive;

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
    public static void setStation(final StationAPI station) {
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
        synchronized (MainApp.class) {
            if (force || mAllRecords == null) {
                // Forget old records and replace them with new
                mAllRecords = records;
                return;
            }
            // Check if old list has more records then new
            if (mDatabase != null && mAllRecords.size() > records.size()) {
                // Reload records from local database
                // (it is better to lose some records statuses then the whole records)
                mAllRecords = mDatabase.loadRecords();
            } else {
                mAllRecords = records;
            }
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
     * Switch to russian locale in debug version.
     *
     * @param context Application context
     * @return Modified context with new russian locale
     */
    public static Context switchToRussian(final Context context) {
        final Locale locale = new Locale("ru");
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final Configuration configuration = context.getResources().getConfiguration();
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        }
        final Resources resources = context.getResources();
        final Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    /**
     * Get mCPActivityActive flag value.
     *
     * @return True if ControlPointActivity is running in foreground.
     */
    public static boolean isCPActivityActive() {
        return mCPActivityActive;
    }

    /**
     * Set mCPActivityActive flag value.
     *
     * @param isActive True if ControlPointActivity is/going to be in foreground
     */
    public static void setCPActivityActive(final boolean isActive) {
        mCPActivityActive = isActive;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String startupError = "";
        final Context context = getApplicationContext();
        // Try to open/create local SQLite database
        try {
            setDatabase(new Database(context));
        } catch (IOException e) {
            startupError = context.getString(R.string.err_db_cant_create_dir).concat(e.getMessage());
        } catch (SQLiteException e) {
            startupError = e.getMessage();
        }
        // Try to load distance amd teams from database if it is not empty
        if ("".equals(startupError) && mDatabase.getDbStatus() == Database.DB_STATE_OK) {
            try {
                final Distance distance =
                        mDatabase.loadDistance(context.getString(R.string.mode_chip_init));
                if (distance != null && !distance.hasErrors()) {
                    setDistance(distance);
                    // Get user email, password and test/main database flag from loaded distance
                    mUIState.setAuthorizationParameters(mDistance.getUserEmail(), mDistance.getUserPassword(),
                            mDistance.getTestSite());
                }
                final Teams teams = mDatabase.loadTeams();
                if (teams != null && !teams.hasErrors()) {
                    setTeams(teams);
                }
            } catch (SQLiteException e) {
                startupError = e.getMessage();
            }
        }
        // Try to load records from database
        if (mDatabase.getDbStatus() == Database.DB_STATE_OK
                || mDatabase.getDbStatus() == Database.DB_STATE_EMPTY) {
            try {
                setAllRecords(mDatabase.loadRecords(), true);
            } catch (SQLiteException e) {
                startupError = e.getMessage();
            }
        }
        // Display startup error (if any)
        if (!"".equals(startupError)) {
            Toast.makeText(this, startupError, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void attachBaseContext(final Context base) {
        // Set russian locale for debug build type
        if (BuildConfig.DEBUG) {
            super.attachBaseContext(switchToRussian(base));
        } else {
            super.attachBaseContext(base);
        }
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
