package ru.mmb.sportiduinomanager.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ru.mmb.sportiduinomanager.R;

import static android.database.DatabaseUtils.sqlEscapeString;

/**
 * Creates the distance from scratch, loads it from local SQLite database
 * and saves it to database.
 */
public class Distance {
    /**
     * Local database state: unknown.
     */
    public static final int DB_STATE_UNKNOWN = 1;
    /**
     * Local database state: damaged or not consistent.
     */
    public static final int DB_STATE_FAILED = 0;
    /**
     * Local database state: just created without any data.
     */
    public static final int DB_STATE_EMPTY = 1;
    /**
     * Local database state: contains data from previous raid.
     */
    public static final int DB_STATE_OUTDATED = 2;
    /**
     * Local database state: healthy.
     */
    public static final int DB_STATE_OK = 3;
    /**
     * Local database structure version.
     */
    private static final int DB_VERSION = 1;
    /**
     * Raid_id from website database.
     */
    private int mRaidId;
    /**
     * Raid_name from website database.
     */
    private String mRaidName;
    /**
     * Unixtime when distance has been downloaded from site.
     */
    private long mTimeDownloaded;
    /**
     * Unixtime when website database will(was) become readonly.
     */
    private long mTimeReadonly;
    /**
     * Unixtime of last active point closing time.
     */
    private long mTimeFinish;
    /**
     * Email of authorized user who performs all interaction with website.
     */
    private String mUserEmail;
    /**
     * Password of authorized user who performs all interaction with website.
     */
    private String mUserPassword;
    /**
     * Which website database is used, test or main.
     */
    private int mTestSite;
    /**
     * Sparse array of active points, array index == point number.
     */
    private Point[] mPoints;
    /**
     * Sparse array of teams, array index == team number.
     */
    private Team[] mTeams;
    /**
     * List of discounts.
     */
    private Discount[] mDiscounts;

    /**
     * Construct distance from imported data.
     *
     * @param raidId       ID of the raid
     * @param readonly     Time when the raid becomes readonly
     * @param finish       Time when the last active point is closed
     * @param raidName     ASCII raid name
     * @param userEmail    Email of the user downloading the raid
     * @param userPassword Password of the user downloading the raid
     * @param testSite     Download raid test site instead of main site
     */
    public Distance(final int raidId, final long readonly, final long finish, final String raidName,
                    final String userEmail, final String userPassword, final int testSite) {
        mRaidId = raidId;
        mRaidName = raidName;
        mTimeDownloaded = System.currentTimeMillis() / 1000;
        mTimeReadonly = readonly;
        mTimeFinish = finish;
        mUserEmail = userEmail;
        mUserPassword = userPassword;
        mTestSite = testSite;
    }

    /**
     * Construct distance from database.
     * Distance should be validated with hasErrors() after using this constructor.
     *
     * @param database SQLite database handler
     */
    public Distance(final SQLiteDatabase database) {
        Cursor result;
        // Load raid parameters
        try {
            result = database.rawQuery("SELECT user_email, user_password, test_site, raid_id,"
                    + " raid_name, unixtime_downloaded, unixtime_readonly, unixtime_finish FROM mmb", null);
        } catch (SQLiteException e) {
            return;
        }
        if (!result.moveToFirst()) {
            result.close();
            return;
        }
        mUserEmail = result.getString(0);
        mUserPassword = result.getString(1);
        mTestSite = result.getInt(2);
        mRaidId = result.getInt(3);
        mRaidName = result.getString(4);
        mTimeDownloaded = result.getLong(5);
        mTimeReadonly = result.getLong(6);
        mTimeFinish = result.getLong(7);
        result.close();
        // Get max point number of reservation of points array
        try {
            result = database.rawQuery("SELECT MAX(number) FROM points", null);
        } catch (SQLiteException e) {
            return;
        }
        if (!result.moveToFirst()) {
            result.close();
            return;
        }
        final int maxPoint = result.getInt(0);
        result.close();
        mPoints = new Point[maxPoint + 1];
        // Load list of points
        try {
            result = database.rawQuery("SELECT number, type, penalty, unixtime_start, "
                    + "unixtime_end, name FROM points", null);
        } catch (SQLiteException e) {
            return;
        }
        result.moveToFirst();
        do {
            mPoints[result.getInt(0)] = new Point(result.getInt(1), result.getInt(2),
                    result.getLong(3), result.getLong(4), result.getString(5));
        } while (result.moveToNext());
        result.close();
        // Get max team number of reservation of teams array
        try {
            result = database.rawQuery("SELECT MAX(number) FROM teams", null);
        } catch (SQLiteException e) {
            return;
        }
        if (!result.moveToFirst()) {
            result.close();
            return;
        }
        final int maxTeam = result.getInt(0);
        result.close();
        mTeams = new Team[maxTeam + 1];
        // Load list of teams
        try {
            result = database.rawQuery("SELECT number, COUNT(*), maps, teams.name FROM teams, "
                    + "members WHERE teams.number = members.team GROUP BY teams.number", null);
        } catch (SQLiteException e) {
            return;
        }
        result.moveToFirst();
        do {
            mTeams[result.getInt(0)] = new Team(result.getInt(1), result.getInt(2),
                    result.getString(3));
        } while (result.moveToNext());
        result.close();
        // Add members to loaded teams
        try {
            result = database.rawQuery("SELECT id, team, name, phone FROM members", null);
        } catch (SQLiteException e) {
            return;
        }
        result.moveToFirst();
        do {
            final int teamN = result.getInt(1);
            final int numberOfMembers = mTeams[teamN].mMembers.length;
            for (int i = 0; i <= numberOfMembers; i++) {
                if (i == numberOfMembers) return;
                if (mTeams[teamN].mMembers[i] == null) {
                    mTeams[teamN].mMembers[i] = new Member(result.getInt(0), result.getString(2),
                            result.getString(3));
                    break;
                }
            }
        } while (result.moveToNext());
        result.close();
        // Get number of discounts
        try {
            result = database.rawQuery("SELECT COUNT(*) FROM discounts", null);
        } catch (SQLiteException e) {
            return;
        }
        if (!result.moveToFirst()) {
            result.close();
            return;
        }
        final int numberOfDiscounts = result.getInt(0);
        result.close();
        mDiscounts = new Discount[numberOfDiscounts];
        if (numberOfDiscounts == 0) return;
        // Load discounts
        try {
            result = database.rawQuery("SELECT minutes, from_point, to_point FROM discounts", null);
        } catch (SQLiteException e) {
            return;
        }
        result.moveToFirst();
        for (int i = 0; i < numberOfDiscounts; i++) {
            mDiscounts[i] = new Discount(result.getInt(0), result.getInt(1), result.getInt(2));
        }
        result.close();
    }

    /**
     * Check database and return it's status.
     *
     * @param database SQLite database handler
     * @return Database status
     */
    public static int getDbStatus(final SQLiteDatabase database) {
        // Check if it the database was opened
        if (database == null) return DB_STATE_FAILED;
        // Check if it has main table with mmb status
        try {
            final Cursor result = database.rawQuery("SELECT name FROM sqlite_master WHERE "
                    + "type='table' AND name='mmb'", null);
            if (result.getCount() != 1) {
                result.close();
                return DB_STATE_EMPTY;
            }
            result.close();
        } catch (SQLiteException e) {
            // Database has wrong structure, erase it
            erase(database);
            return DB_STATE_EMPTY;
        }
        // Get database version, test flag and download date
        try {
            final Cursor result = database.rawQuery("SELECT version FROM mmb", null);
            if (!result.moveToFirst()) {
                // database is damaged, erase all data
                result.close();
                erase(database);
                return DB_STATE_EMPTY;
            }
            // Check database version
            if (result.getInt(0) != DB_VERSION) {
                // database has previous version of data structures , erase all data
                result.close();
                erase(database);
                return DB_STATE_EMPTY;
            }
            result.close();
        } catch (SQLiteException e) {
            // Database has wrong structure, erase it
            erase(database);
            return DB_STATE_EMPTY;
        }
        // check if database can be outdated
        try {

            final Cursor result = database.rawQuery("SELECT (NOT test_site) AND "
                            + "(unixtime_downloaded < unixtime_readonly) FROM mmb",
                    null);
            if (!result.moveToFirst()) {
                // database is damaged, erase all data
                result.close();
                erase(database);
                return DB_STATE_EMPTY;
            }
            if (result.getInt(0) == 1) {
                result.close();
                return DB_STATE_OUTDATED;
            }
            result.close();
        } catch (SQLiteException e) {
            // Database has wrong structure, erase it
            erase(database);
            return DB_STATE_EMPTY;
        }
        // All checks passed
        return DB_STATE_OK;
    }

    /**
     * Erase all tables in database when it has old structure or was broken.
     *
     * @param database Database handle
     */
    private static void erase(final SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS mmb");
        database.execSQL("DROP TABLE IF EXISTS points");
        database.execSQL("DROP TABLE IF EXISTS teams");
        database.execSQL("DROP TABLE IF EXISTS members");
        database.execSQL("DROP TABLE IF EXISTS discounts");
        // TODO: drop other tables
    }

    /**
     * Get descriptive string message about database status.
     *
     * @param status Status of the database
     * @return Resource id with string message about the status
     */
    public static int getStatusMessage(final int status) {
        switch (status) {
            case DB_STATE_FAILED:
                return R.string.database_fatal_error;
            case DB_STATE_EMPTY:
                return R.string.database_empty;
            case DB_STATE_OUTDATED:
                return R.string.database_outdated;
            case DB_STATE_OK:
                return R.string.database_ok;
            default:
                return R.string.database_status_unknown;
        }
    }

    /**
     * Get email of the authorized user who downloaded the distance from website.
     *
     * @return User email
     */
    public String getUserEmail() {
        return mUserEmail;
    }

    /**
     * Get password of the authorized user who downloaded the distance from website.
     *
     * @return User password
     */
    public String getUserPassword() {
        return mUserPassword;
    }

    /**
     * Get info about which website database we are using, main of test.
     *
     * @return Test site flag
     */
    public int getTestSite() {
        return mTestSite;
    }

    /**
     * Get name of current raid.
     *
     * @return Raid name
     */
    public String getRaidName() {
        return mRaidName;
    }

    /**
     * Get time of downloading distance from site.
     *
     * @return Formatted datetime
     */
    public String getDownloadDate() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mTimeDownloaded * 1000);
        final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return format.format(calendar.getTime());
    }

    /**
     * Get list of active points names.
     *
     * @return List of names
     */
    public List<String> getPointNames() {
        final List<String> names = new ArrayList<>();
        for (final Point point : mPoints) {
            names.add(point.mName);
        }
        return names;
    }

    /**
     * Get the point type.
     *
     * @param number Point number
     * @return Point type
     */
    public int getPointType(final int number) {
        if (number < 0 || number >= mPoints.length) return -1;
        return mPoints[number].mType;
    }

    /**
     * Get the team name.
     *
     * @param number Team number
     * @return String with team name or null if the team does not exist
     */
    public String getTeamName(final int number) {
        if (number <= 0 || number >= mTeams.length) return null;
        if (mTeams[number] == null) return null;
        return mTeams[number].mName;
    }

    /**
     * Get the team maps count.
     *
     * @param number Team number
     * @return Number of maps for the team
     */
    public int getTeamMaps(final int number) {
        if (number <= 0 || number >= mTeams.length) return 0;
        if (mTeams[number] == null) return 0;
        return mTeams[number].mMaps;
    }

    /**
     * Get the list of team members names.
     *
     * @param number Team number
     * @return List of team members names (or empty list if the team does not exist)
     */
    public List<String> getTeamMembers(final int number) {
        if (number <= 0 || number >= mTeams.length) return new ArrayList<>();
        if (mTeams[number] == null) return new ArrayList<>();
        final List<String> list = new ArrayList<>();
        for (final Member member : mTeams[number].mMembers) {
            list.add(member.mName);
        }
        return list;
    }

    /**
     * Save distance to local SQLite database.
     *
     * @param database SQLite database handler
     * @return SQLite exception or null in case of success
     */
    public String saveToDb(final SQLiteDatabase database) {
        // Create main table with raid parameters
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS mmb(version INTEGER NOT NULL,"
                    + " user_email VARCHAR(100) NOT NULL, user_password VARCHAR(35) NOT NULL,"
                    + " test_site INTEGER NOT NULL, unixtime_downloaded INTEGER NOT NULL,"
                    + " raid_id INTEGER PRIMARY KEY, raid_name VARCHAR(50) NOT NULL,"
                    + " unixtime_readonly INTEGER NOT NULL, unixtime_finish INTEGER NOT NULL)");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Empty the table just in case
        try {
            database.execSQL("DELETE FROM mmb");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Save general raid parameters into database
        try {
            final String sql = String.format(Locale.getDefault(), "INSERT INTO mmb(version, user_email,"
                            + " user_password, test_site, unixtime_downloaded, raid_id, raid_name, unixtime_readonly, "
                            + " unixtime_finish) VALUES (%d, %s, %s, %d, %d, %d, %s, %d, %d)",
                    DB_VERSION, sqlEscapeString(mUserEmail), sqlEscapeString(mUserPassword),
                    mTestSite, mTimeDownloaded, mRaidId, sqlEscapeString(mRaidName),
                    mTimeReadonly, mTimeFinish);
            database.execSQL(sql);
        } catch (SQLiteException e) {
            return e.getMessage();
        }

        // Create table with points parameters
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS points(number INTEGER PRIMARY KEY,"
                    + " type INTEGER NOT NULL, penalty INTEGER NOT NULL,"
                    + " unixtime_start DATETIME NOT NULL, unixtime_end DATETIME NOT NULL,"
                    + " name VARCHAR(50) NOT NULL)");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Empty the table just in case
        try {
            database.execSQL("DELETE FROM points");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Save all points (including zero point for chip initialization) into database
        for (int i = 0; i < mPoints.length; i++) {
            final Point point = mPoints[i];
            if (point == null) continue;
            try {
                final String sql = String.format(Locale.getDefault(), "INSERT INTO points(number, type, penalty,"
                                + " unixtime_start, unixtime_end, name) VALUES(%d, %d, %d, %d, %d, %s)",
                        i, point.mType, point.mPenalty, point.mStart, point.mEnd,
                        sqlEscapeString(point.mName));
                database.execSQL(sql);
            } catch (SQLiteException e) {
                return e.getMessage();
            }
        }

        // Create table with teams parameters
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS teams(number INTEGER PRIMARY KEY, maps INTEGER NOT NULL,"
                    + " name VARCHAR(100))");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Empty the table just in case
        try {
            database.execSQL("DELETE FROM teams");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Create table with teams members
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS members(id INTEGER PRIMARY KEY, team integer,"
                    + " name VARCHAR(105), phone varchar(25))");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Empty tables just in case
        try {
            database.execSQL("DELETE FROM teams");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        try {
            database.execSQL("DELETE FROM members");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Save all teams and members into database
        for (int i = 0; i < mTeams.length; i++) {
            final Team team = mTeams[i];
            if (team == null) continue;
            try {
                final String sql = String.format(Locale.getDefault(), "INSERT INTO teams(number, maps, name)"
                        + " VALUES(%d, %d, %s)", i, team.mMaps, sqlEscapeString(team.mName));
                database.execSQL(sql);
            } catch (SQLiteException e) {
                return e.getMessage();
            }
            for (final Member member : team.mMembers) {
                try {
                    final String sql = String.format(Locale.getDefault(),
                            "INSERT INTO members(id, team, name, phone)"
                                    + " VALUES(%d, %d, %s, %s)", member.mId, i,
                            sqlEscapeString(member.mName), sqlEscapeString(member.mPhone));
                    database.execSQL(sql);
                } catch (SQLiteException e) {
                    return e.getMessage();
                }
            }
        }

        // Create table with discounts
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS discounts(minutes INTEGER NOT NULL,"
                    + " from_point INTEGER NOT NULL, to_point INTEGER NOT NULL)");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Empty the table just in case
        try {
            database.execSQL("DELETE FROM discounts");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // Save discounts into database
        for (final Discount discount : mDiscounts) {
            try {
                final String sql = String.format(Locale.getDefault(), "INSERT INTO discounts(minutes, from_point,"
                        + " to_point) VALUES(%d, %d, %d)", discount.mMinutes, discount.mFrom, discount.mTo);
                database.execSQL(sql);
            } catch (SQLiteException e) {
                return e.getMessage();
            }
        }

        // process journal and clean up the database file
        try {
            database.execSQL("VACUUM");
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        return null;
    }

    /**
     * Check if the database can be reloaded from server loosing all current data.
     *
     * @return True if it can be reloaded
     */
    public boolean canBeReloaded() {
        // Allow data loss if it was not initialized correctly
        if (mTimeReadonly == 0 || mTimeFinish == 0) return true;
        // Get current time
        final long now = System.currentTimeMillis() / 1000L;
        // Allow data loss if distance was not set readonly yet
        // (we can and should reload it)
        if (now < mTimeReadonly) return true;
        // Allow data loss if race was finished more then 1 month ago
        // (race was finalized anyway)
        return now > (mTimeFinish + 3600 * 24 * 30);
        // TODO: Add check for team results which were not uploaded to server
    }

    /**
     * Allocate point array with maxIndex as max array index.
     *
     * @param maxIndex       Max index in point array
     * @param initChipsPoint Name of pseudo point for chip initialization
     */
    public void initPointArray(final int maxIndex, final String initChipsPoint) {
        mPoints = new Point[maxIndex + 1];
        mPoints[0] = new Point(0, 0, 0, 0, initChipsPoint);
    }

    /**
     * Allocate team array with maxNumber as max array index.
     *
     * @param maxNumber Max index in team array
     */
    public void initTeamArray(final int maxNumber) {
        mTeams = new Team[maxNumber + 1];
    }

    /**
     * Allocate discount array with nDiscount as array size.
     *
     * @param numberOfDiscounts Number of discounts
     */
    public void initDiscountArray(final int numberOfDiscounts) {
        mDiscounts = new Discount[numberOfDiscounts];
    }

    /**
     * Construct point and save it to appropriate position in point array.
     *
     * @param index   Position in point array
     * @param type    Point type (start, finish, etc)
     * @param penalty Penalty for missing the point
     * @param start   Unixtime when point starts registering of teams
     * @param end     Unixtime when point ends registering of teams
     * @param name    Point name
     * @return True in case of valid index value
     */
    public boolean addPoint(final int index, final int type, final int penalty, final long start, final long end,
                            final String name) {
        // Check if point array was initialized
        if (mPoints == null) return false;
        // Check if point index is valid
        if (index <= 0 || index >= mPoints.length) return false;
        // Check if the point was already set
        if (mPoints[index] == null) {
            // set the point
            mPoints[index] = new Point(type, penalty, start, end, name);
            return true;
        }
        return false;
    }

    /**
     * Construct team and save it to appropriate position in team array.
     *
     * @param number       Team number
     * @param membersCount Number of team members
     * @param mapsCount    Number of maps
     * @param name         Team name
     * @return True in case of valid team number value
     */
    public boolean addTeam(final int number, final int membersCount, final int mapsCount,
                           final String name) {
        // Check if team array was initialized
        if (mTeams == null) return false;
        // Check if team number is valid
        if (number < 0 || number >= mTeams.length) return false;
        // Check if the point was already set
        if (mTeams[number] == null) {
            // set the point
            mTeams[number] = new Team(membersCount, mapsCount, name);
            return true;
        }
        return false;
    }

    /**
     * Add new member to the list of team members.
     *
     * @param memberId Member id
     * @param team     Team number
     * @param name     Member first and last name and year of birth
     * @param phone    Member mobile phone (can be empty)
     * @return True in case of valid team and number of its members
     */
    public boolean addMember(final long memberId, final int team, final String name, final String phone) {
        // Check if team array was initialized
        if (mTeams == null) return false;
        // Check if team number is valid
        if (team < 0 || team >= mTeams.length) return false;
        // Check if the team was initialized
        if (mTeams[team] == null) return false;
        // Try to add member to free place in team members array
        for (int i = 0; i < mTeams[team].mMembers.length; i++) {
            if (mTeams[team].mMembers[i] == null) {
                mTeams[team].mMembers[i] = new Member(memberId, name, phone);
                return true;
            }
        }
        return false;
    }

    /**
     * Add new discount to the list of discounts.
     *
     * @param minutes   The discount
     * @param fromPoint First point of discount interval
     * @param toPoint   Last point of discount interval
     * @return True in case of success
     */
    public boolean addDiscount(final int minutes, final int fromPoint, final int toPoint) {
        // Check if discount array was initialized
        if (mDiscounts == null) return false;
        for (int i = 0; i < mDiscounts.length; i++) {
            if (mDiscounts[i] == null) {
                mDiscounts[i] = new Discount(minutes, fromPoint, toPoint);
                return true;
            }
        }
        return false;
    }

    /**
     * Check the distance (loaded from site or from local db) for various errors.
     *
     * @return True if some errors were found
     */
    public boolean hasErrors() {
        // Check distance parameters
        if (mRaidId <= 0) return true;
        if (mTimeReadonly <= 0) return true;
        if (mTimeFinish <= 0) return true;
        if (mTimeFinish <= mTimeReadonly) return true;
        if ("".equals(mRaidName)) return true;

        // Check if some points were loaded
        if (mPoints == null) return true;
        if (mPoints.length <= 1) return true;
        // Check if all points were loaded
        if (mPoints[0] == null) return true;
        if (mPoints[mPoints.length - 1] == null) return true;
        // check point data
        for (int i = 1; i < mPoints.length; i++) {
            if (mPoints[i] != null) {
                if (mPoints[i].mType <= 0 || mPoints[i].mType > 5) return true;
                if (mPoints[i].mPenalty < 0) return true;
                if (mPoints[i].mStart > 0 && mPoints[i].mEnd < mPoints[i].mStart) return true;
                if ("".equals(mPoints[i].mName)) return true;
            }
        }

        // Check if some teams were loaded
        if (mTeams == null) return true;
        if (mTeams.length == 0) return true;
        // Check if all teams were loaded
        if (mTeams[mTeams.length - 1] == null) return true;
        // Check teams data
        for (final Team team : mTeams) {
            if (team != null) {
                // Check if all team members were loaded
                for (int i = 0; i < team.mMembers.length; i++) {
                    if (team.mMembers[i] == null) return true;
                    // Check for bad member data
                    if (team.mMembers[i].mId <= 0) return true;
                    if ("".equals(team.mMembers[i].mName)) return true;
                }
                // Check number of maps
                if (team.mMaps <= 0) return true;
                // Check for empty team name
                if ("".equals(team.mName)) return true;
            }
        }

        // Check if some discounts were loaded
        if (mDiscounts == null) return true;
        if (mDiscounts.length == 0) return false;
        // Check discounts data
        for (final Discount discount : mDiscounts) {
            // Check if all discounts were loaded
            if (discount == null) return true;
            // Check discount value
            if (discount.mMinutes <= 0) return true;
            // Check discount interval
            if (discount.mFrom <= 0 || discount.mFrom >= mPoints.length) return true;
            if (discount.mTo <= 0 || discount.mTo >= mPoints.length) return true;
            if (discount.mFrom >= discount.mTo) return true;
            if (mPoints[discount.mFrom] == null) return true;
            if (mPoints[discount.mTo] == null) return true;
        }

        // No errors were detected
        return false;
    }

    /*
    public String getPointName(final int index) {
        if (index < 0 || index >= mPoints.size()) return UNKNOWN_POINT;
        Point point = mPoints.get(index);
        if (point == null) return UNKNOWN_POINT;
        return point.mName;
    }*/

    /**
     * An active point parameters.
     */
    private class Point {
        /**
         * Point type (start, finish, etc).
         */
        final int mType;
        /**
         * Penalty in minutes for missing the point.
         */
        final int mPenalty;
        /**
         * Unixtime when the point starts to work.
         */
        final long mStart;
        /**
         * Unixtime when the point ends working.
         */
        final long mEnd;
        /**
         * Point name.
         */
        final String mName;

        /**
         * Constructor for Point class.
         *
         * @param type    Point type
         * @param penalty Penalty for missing this point
         * @param start   Time at which this point start working
         * @param end     Time at which this point stop working
         * @param name    Point name
         */
        Point(final int type, final int penalty, final long start, final long end, final String name) {
            mType = type;
            mPenalty = penalty;
            mStart = start;
            mEnd = end;
            mName = name;
        }
    }

    /**
     * A member of a team.
     */
    private class Member {
        /**
         * Member id.
         */
        final long mId;
        /**
         * Member first name, last name and year of birth.
         */
        final String mName;
        /**
         * Member phone (can be empty).
         */
        final String mPhone;

        /**
         * Constructor for Member class.
         *
         * @param memberId ID of the member
         * @param name     Member name and birth date
         * @param phone    Member phone
         */
        Member(final long memberId, final String name, final String phone) {
            mId = memberId;
            mName = name;
            mPhone = phone;
        }
    }

    /**
     * A team parameters.
     */
    private class Team {
        /**
         * Number of maps printed for the team.
         */
        final int mMaps;
        /**
         * Team name.
         */
        final String mName;
        /**
         * List of team members.
         */
        final Member[] mMembers;

        /**
         * Constructor for Team class.
         *
         * @param membersCount Number of members in this team
         * @param mapsCount    Number of ordered maps
         * @param name         Team name
         */
        Team(final int membersCount, final int mapsCount, final String name) {
            mMaps = mapsCount;
            mName = name;
            mMembers = new Member[membersCount];
        }
    }

    /**
     * Discount for missing some points.
     */
    private class Discount {
        /**
         * The discount in minutes.
         */
        final int mMinutes;
        /**
         * First point of the distance part where discount is active.
         */
        final int mFrom;
        /**
         * Last point of the interval.
         */
        final int mTo;

        /**
         * Constructor for Discount class.
         *
         * @param minutes   Value of discount in minutes
         * @param fromPoint Starting point for discount interval
         * @param toPoint   Ending point for discount interval
         */
        Discount(final int minutes, final int fromPoint, final int toPoint) {
            mMinutes = minutes;
            mFrom = fromPoint;
            mTo = toPoint;
        }
    }
}
