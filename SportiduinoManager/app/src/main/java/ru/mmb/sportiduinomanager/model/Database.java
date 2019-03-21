package ru.mmb.sportiduinomanager.model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Init local SQLite database and load/save data from/to it.
 */
public final class Database {
    /**
     * Local database state: Can't be created.
     */
    public static final int DB_STATE_FAILED = 0;
    /**
     * Local database state: just created without any data.
     */
    public static final int DB_STATE_EMPTY = 1;
    /**
     * Local database state: contains some distance.
     */
    public static final int DB_STATE_OK = 2;
    /**
     * Local database state: contains some distance.
     */
    public static final int DB_STATE_DAMAGED = 3;
    /**
     * Name of local SQLite database.
     */
    private static final String DB_NAME = "mmb.sqlite";
    /**
     * SQLite database locale.
     */
    private static final String LOCALE = "ru_RU";
    /**
     * Local database structure version.
     */
    private static final int DB_VERSION = 2;

    /**
     * Name of SQLite database file.
     */
    private final String mPath;
    /**
     * Current status of SQLite database.
     */
    private int mDbStatus;

    /**
     * Create new database at first run, check its version, check if it has a distance
     * and recreate it's tables in case of fatal errors.
     *
     * @param context Application context to detect database path
     * @throws IOException     Thrown when database folder can't be created
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    public Database(final Context context) throws IOException, SQLiteException {
        // Get default path to application databases
        // and create databases folder if not exist
        final File databasePath = context.getDatabasePath(DB_NAME);
        final File folder = databasePath.getParentFile();
        if (folder != null && !folder.exists()) {
            final boolean success = folder.mkdir();
            if (!success) {
                throw new IOException(folder.getAbsolutePath());
            }
        }
        mPath = databasePath.getAbsolutePath();
        // Try to open database (it will be created if it does not exist)
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath,
                null, SQLiteDatabase.CREATE_IF_NECESSARY);
        database.setLocale(new Locale(LOCALE));
        // Check database version
        try {
            final Cursor result = database.rawQuery("SELECT version FROM mmb", null);
            if (result.moveToFirst() && result.getInt(0) == DB_VERSION) {
                // Database has correct version
                result.close();
            } else {
                // Database is damaged or has previous version of data structures , recreate it
                result.close();
                recreateTables(database);
            }
        } catch (SQLiteException e) {
            // Database has wrong structure, recreate it
            recreateTables(database);
        }
        // Check if we have a distance in database
        try {
            final Cursor result = database.rawQuery("SELECT COUNT(*) FROM distance", null);
            result.moveToFirst();
            final int distanceCount = result.getInt(0);
            result.close();
            if (distanceCount == 1) {
                mDbStatus = DB_STATE_OK;
            } else {
                if (distanceCount == 0) {
                    mDbStatus = DB_STATE_EMPTY;
                } else {
                    // Database has several distances, erase it
                    recreateTables(database);
                }
            }
        } catch (SQLiteException e) {
            // Something gone wrong, recreate database
            recreateTables(database);
        }
        // Close the database
        database.close();
    }

    /**
     * Get local SQLite database status.
     *
     * @return The status
     */
    public int getDbStatus() {
        return mDbStatus;
    }

    /**
     * Load distance from local SQLite database.
     *
     * @param initChipsPoint Name of chip initialization point from app resources
     * @return New distance object with loaded data or null in case of an error
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    public Distance loadDistance(final String initChipsPoint) throws SQLiteException {
        Cursor result;
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READONLY);
        // Load raid parameters
        result = database.rawQuery("SELECT user_email, user_password, test_site, raid_id,"
                        + " raid_name, unixtime_downloaded, unixtime_readonly, unixtime_finish "
                        + "FROM distance",
                null);
        if (!result.moveToFirst()) {
            result.close();
            database.close();
            return null;
        }
        // Create new distance (without points and discounts yet)
        final Distance distance = new Distance(result.getString(0), result.getString(1),
                result.getInt(2), result.getInt(3), result.getString(4), result.getLong(5),
                result.getLong(6), result.getLong(7));
        result.close();
        // Get max point number for reservation of points array
        result = database.rawQuery("SELECT MAX(number) FROM points", null);
        int maxPointNumber;
        if (result.moveToFirst()) {
            maxPointNumber = result.getInt(0);
        } else {
            maxPointNumber = 0;
        }
        distance.initPointArray(maxPointNumber, initChipsPoint);
        result.close();
        // Load list of points
        result = database.rawQuery("SELECT number, type, penalty, unixtime_start, "
                + "unixtime_end, name FROM points", null);
        result.moveToFirst();
        do {
            distance.addPoint(result.getInt(0), result.getInt(1), result.getInt(2),
                    result.getLong(3), result.getLong(4), result.getString(5));
        } while (result.moveToNext());
        result.close();
        // Get number of discounts
        result = database.rawQuery("SELECT COUNT(*) FROM discounts", null);
        if (!result.moveToFirst()) {
            result.close();
            database.close();
            return null;
        }
        final int numberOfDiscounts = result.getInt(0);
        result.close();
        distance.initDiscountArray(numberOfDiscounts);
        // Load discounts
        if (numberOfDiscounts > 0) {
            result = database.rawQuery("SELECT minutes, from_point, to_point FROM discounts", null);
            result.moveToFirst();
            for (int i = 0; i < numberOfDiscounts; i++) {
                distance.addDiscount(result.getInt(0), result.getInt(1), result.getInt(2));
            }
            result.close();
        }
        database.close();
        return distance;
    }

    /**
     * Load teams and teams members from local SQLite database.
     *
     * @return New teams object with loaded data or null in case of an error
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    public Teams loadTeams() throws SQLiteException {
        Cursor result;
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READONLY);
        // Get max team number of reservation of teams array
        result = database.rawQuery("SELECT MAX(number) FROM teams", null);
        if (!result.moveToFirst()) {
            result.close();
            database.close();
            return null;
        }
        final int maxTeam = result.getInt(0);
        result.close();
        // Create teams object
        final Teams teams = new Teams(maxTeam);
        // Load list of teams
        result = database.rawQuery("SELECT number, COUNT(*), maps, teams.name FROM teams, "
                + "members WHERE teams.number = members.team GROUP BY teams.number", null);
        result.moveToFirst();
        do {
            if (!teams.addTeam(result.getInt(0), result.getInt(1), result.getInt(2),
                    result.getString(3))) {
                result.close();
                database.close();
                return null;
            }
        } while (result.moveToNext());
        result.close();
        // Add members to loaded teams
        result = database.rawQuery("SELECT id, team, name, phone FROM members ORDER BY id ASC",
                null);
        if (!result.moveToFirst()) {
            result.close();
            database.close();
            return null;
        }
        do {
            if (!teams.addTeamMember(result.getLong(0), result.getInt(1), result.getString(2),
                    result.getString(3))) {
                result.close();
                database.close();
                return null;
            }
        } while (result.moveToNext());
        result.close();
        // Teams were loaded
        database.close();
        return teams;
    }

    /**
     * Load chips events from local SQLite database.
     *
     * @return New Chips object with loaded data or null in case of an error
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    public Chips loadChips() throws SQLiteException {
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READONLY);
        // Get distance download date
        Cursor result = database.rawQuery("SELECT unixtime_downloaded FROM distance", null);
        long timeDownloaded;
        if (result.moveToFirst()) {
            timeDownloaded = result.getLong(0);
        } else {
            timeDownloaded = 0;
        }
        result.close();
        // Create chips object
        final Chips chips = new Chips(timeDownloaded);
        // Load chip events into it
        result = database.rawQuery("SELECT stationmac, stationtime, stationdrift,"
                + " stationnumber, stationmode, inittime, team_num, teammask, levelpoint_order,"
                + " teamlevelpoint_datetime, status FROM chips", null);
        if (!result.moveToFirst()) {
            // No chips events in database yet
            result.close();
            database.close();
            return chips;
        }
        do {
            final ChipEvent event = new ChipEvent(result.getLong(0), result.getInt(1),
                    result.getInt(2), result.getInt(3), result.getInt(4), result.getInt(5),
                    result.getInt(6), result.getInt(7), result.getInt(8), result.getInt(9),
                    result.getInt(10));
            chips.addEvent(event);
        } while (result.moveToNext());
        result.close();
        database.close();
        return chips;
    }


    /**
     * Save distance to local SQLite database.
     *
     * @param distance A distance to save
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    void saveDistance(final Distance distance) throws SQLiteException {
        SQLiteStatement statement;
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READWRITE);
        // Empty the table with raid parameters
        database.execSQL("DELETE FROM distance");
        // Save general raid parameters into database
        statement = database.compileStatement("INSERT INTO distance(user_email, user_password,"
                + " test_site, unixtime_downloaded, raid_id, raid_name,"
                + " unixtime_readonly, unixtime_finish) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        statement.bindString(1, distance.getUserEmail());
        statement.bindString(2, distance.getUserPassword());
        statement.bindLong(3, distance.getTestSite());
        statement.bindLong(4, distance.getTimeDownloaded());
        statement.bindLong(5, distance.getRaidId());
        statement.bindString(6, distance.getRaidName());
        statement.bindLong(7, distance.getTimeReadonly());
        statement.bindLong(8, distance.getTimeFinish());
        statement.execute();
        // Empty the table with points
        database.execSQL("DELETE FROM points");
        // Save all points (excluding zero point for chip initialization) into database
        final List<Integer> numbers = new ArrayList<>();
        final List<Integer> types = new ArrayList<>();
        final List<Integer> penalties = new ArrayList<>();
        final List<Long> startTimes = new ArrayList<>();
        final List<Long> endTimes = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        distance.fillPointsLists(numbers, types, penalties, startTimes, endTimes, names);
        statement = database.compileStatement("INSERT INTO points(number, type, penalty,"
                + " unixtime_start, unixtime_end, name) VALUES(?, ?, ?, ?, ?, ?)");
        for (int i = 0; i < numbers.size(); i++) {
            statement.bindLong(1, numbers.get(i));
            statement.bindLong(2, types.get(i));
            statement.bindLong(3, penalties.get(i));
            statement.bindLong(4, startTimes.get(i));
            statement.bindLong(5, endTimes.get(i));
            statement.bindString(6, names.get(i));
            statement.execute();
        }
        // Empty the table with discounts
        database.execSQL("DELETE FROM discounts");
        // Save discounts into database
        final List<Integer> minutes = new ArrayList<>();
        final List<Integer> fromN = new ArrayList<>();
        final List<Integer> toN = new ArrayList<>();
        distance.fillDiscountsLists(minutes, fromN, toN);
        statement = database.compileStatement("INSERT INTO discounts(minutes, from_point,"
                + " to_point) VALUES(?, ?, ?)");
        for (int i = 0; i < minutes.size(); i++) {
            statement.bindLong(1, minutes.get(i));
            statement.bindLong(2, fromN.get(i));
            statement.bindLong(3, toN.get(i));
            statement.execute();
        }
        // process journal and clean up the database file
        database.execSQL("VACUUM");
        database.close();
        mDbStatus = DB_STATE_OK;
    }

    /**
     * Save teams and teams members to local SQLite database.
     *
     * @param teams Teams to save
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    void saveTeams(final Teams teams) throws SQLiteException {
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READWRITE);
        // Empty the table with teams
        database.execSQL("DELETE FROM teams");
        // Empty the table with team members
        database.execSQL("DELETE FROM members");
        // Save teams and team members
        final SQLiteStatement teamStatement = database.compileStatement("INSERT INTO teams"
                + "(number, maps, name) VALUES(?, ?, ?)");
        final SQLiteStatement memberStatement = database.compileStatement("INSERT INTO members"
                + "(id, team, name, phone) VALUES(?, ?, ?, ?)");
        for (int i = 1; i <= teams.getMaxTeam(); i++) {
            // Save team
            final String name = teams.getTeamName(i);
            if (name == null) continue;
            teamStatement.bindLong(1, i);
            teamStatement.bindLong(2, teams.getTeamMaps(i));
            teamStatement.bindString(3, name);
            teamStatement.execute();
            // Save team members
            final List<Long> ids = teams.getMembersIds(i);
            final List<String> names = teams.getMembersNames(i);
            final List<String> phones = teams.getMembersPhones(i);
            for (int j = 0; j < ids.size(); j++) {
                memberStatement.bindLong(1, ids.get(j));
                memberStatement.bindLong(2, i);
                memberStatement.bindString(3, names.get(j));
                memberStatement.bindString(4, phones.get(j));
                memberStatement.execute();
            }
        }
        // process journal and clean up the database file
        database.execSQL("VACUUM");
        database.close();
    }

    /**
     * Save chips events from custom list of events to local SQLite database.
     *
     * @param events List of chips events
     * @throws SQLiteException All SQL exceptions while working with SQLite database
     */
    void saveChips(final List<ChipEvent> events) throws SQLiteException {
        // Open local database
        final SQLiteDatabase database = SQLiteDatabase.openDatabase(mPath, null,
                SQLiteDatabase.OPEN_READWRITE);
        final SQLiteStatement statement = database.compileStatement("INSERT INTO chips"
                + "(stationmac, stationtime, stationdrift, stationnumber, stationmode,"
                + " inittime, team_num, teammask, levelpoint_order,"
                + " teamlevelpoint_datetime, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        // Save all events from the unsaved list
        for (final ChipEvent event : events) {
            statement.bindLong(1, event.mStationMAC);
            statement.bindLong(2, event.mStationTime);
            statement.bindLong(3, event.mStationDrift);
            statement.bindLong(4, event.mStationNumber);
            statement.bindLong(5, event.mStationMode);
            statement.bindLong(6, event.mInitTime);
            statement.bindLong(7, event.mTeamNumber);
            statement.bindLong(8, event.mTeamMask);
            statement.bindLong(9, event.mPointNumber);
            statement.bindLong(10, event.mPointTime);
            statement.bindLong(11, ChipEvent.STATUS_SAVED);
            statement.execute();
        }
        database.close();
    }

    /**
     * Recreate all tables in local SQLite database.
     *
     * @param database Handle of opened SQLite database
     */
    private void recreateTables(final SQLiteDatabase database) {
        // Create the table with database version
        database.execSQL("DROP TABLE IF EXISTS mmb");
        database.execSQL("CREATE TABLE mmb(version INTEGER NOT NULL)");
        database.execSQL("INSERT INTO mmb(version) VALUES (" + DB_VERSION + ")");
        // Create the table with distance parameters
        database.execSQL("DROP TABLE IF EXISTS distance");
        database.execSQL("CREATE TABLE distance(user_email VARCHAR(100) NOT NULL,"
                + " user_password VARCHAR(35) NOT NULL,"
                + " test_site INTEGER NOT NULL, unixtime_downloaded INTEGER NOT NULL,"
                + " raid_id INTEGER PRIMARY KEY, raid_name VARCHAR(50) NOT NULL,"
                + " unixtime_readonly INTEGER NOT NULL, unixtime_finish INTEGER NOT NULL)");
        // Create the table with points list
        database.execSQL("DROP TABLE IF EXISTS points");
        database.execSQL("CREATE TABLE points(number INTEGER PRIMARY KEY,"
                + " type INTEGER NOT NULL, penalty INTEGER NOT NULL,"
                + " unixtime_start DATETIME NOT NULL, unixtime_end DATETIME NOT NULL,"
                + " name VARCHAR(50) NOT NULL)");
        // Create the  table with teams list
        database.execSQL("DROP TABLE IF EXISTS teams");
        database.execSQL("CREATE TABLE teams(number INTEGER PRIMARY KEY, maps INTEGER NOT NULL,"
                + " name VARCHAR(100))");
        // Create the table with teams members
        database.execSQL("DROP TABLE IF EXISTS members");
        database.execSQL("CREATE TABLE members(id INTEGER PRIMARY KEY, team integer,"
                + " name VARCHAR(105), phone varchar(25))");
        // Create the table with discounts
        database.execSQL("DROP TABLE IF EXISTS discounts");
        database.execSQL("CREATE TABLE discounts(minutes INTEGER NOT NULL,"
                + " from_point INTEGER NOT NULL, to_point INTEGER NOT NULL)");
        // Create table with registered chips data
        database.execSQL("DROP TABLE IF EXISTS chips");
        database.execSQL("CREATE TABLE chips(stationmac INTEGER NOT NULL,"
                + " stationtime INTEGER NOT NULL, stationdrift INTEGER NOT NULL,"
                + " stationnumber INTEGER NOT NULL, stationmode INTEGER NOT NULL,"
                + " inittime INTEGER NOT NULL, team_num INTEGER NOT NULL,"
                + " teammask INTEGER NOT NULL, levelpoint_order INTEGER NOT NULL,"
                + " teamlevelpoint_datetime INTEGER NOT NULL, status INTEGER NOT NULL,"
                + " PRIMARY KEY (stationmac, stationtime, stationdrift,"
                + "stationnumber, stationmode, inittime, team_num, teammask, levelpoint_order,"
                + " teamlevelpoint_datetime))");
        // TODO: Recreate other tables
        // process journal and clean up the database file
        database.execSQL("VACUUM");
        // Change the status to empty
        mDbStatus = DB_STATE_EMPTY;
    }

}
