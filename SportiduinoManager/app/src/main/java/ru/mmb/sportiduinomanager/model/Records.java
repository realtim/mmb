package ru.mmb.sportiduinomanager.model;

import android.database.sqlite.SQLiteException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Handling Sportiduino records (initialization/punches) received from stations.
 */
public final class Records {
    /**
     * Used as return string when no SQL error has been occurred.
     */
    private static final String SUCCESS = "";

    /**
     * List of records (initializations and punches).
     */
    private final List<Record> mRecords;

    /**
     * Unixtime when distance has been downloaded from site.
     */
    private final long mDistDownloaded;

    /**
     * Construct empty list of records.
     *
     * @param distDownloaded Time of distance dl (to set in all new records)
     */
    public Records(final long distDownloaded) {
        mRecords = new ArrayList<>();
        mDistDownloaded = distDownloaded;
    }

    /**
     * Convert unixtime to string using fixed UTC+3 locale.
     *
     * @param time   Unixtime to print
     * @param format Print format such as "dd.MM  HH:mm:ss"
     * @return String representation of time
     */
    public static String printTime(final long time, final String format) {
        if (time <= 0) return "-";
        // Use fixed +3 hours offset
        final TimeZone raidTimezone = TimeZone.getTimeZone("GMT+3");
        final Calendar calendar = Calendar.getInstance(raidTimezone);
        calendar.setTimeInMillis(time * 1000L);
        final DateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        dateFormat.setTimeZone(raidTimezone);
        return dateFormat.format(calendar.getTime());
    }

    /**
     * Get previously loaded time of distance download.
     *
     * @return Time as unixtime
     */
    long getTimeDownloaded() {
        return mDistDownloaded;
    }

    /**
     * Get list of all unsent records converted to strings.
     *
     * @return Array of strings with records
     */
    List<String> getUnsentRecords() {
        final List<String> recordsAsString = new ArrayList<>();
        for (final Record record : mRecords) {
            if (record.getStatus() != Record.STATUS_SENT) {
                recordsAsString.add(record.toString());
            }
        }
        return recordsAsString;
    }

    /**
     * Get team number from 'index' element of mRecords list
     * (mRecords should be previously filtered with getPunchesAtStation).
     *
     * @param index Index in mRecords list array
     * @return Team number for element with this index
     */
    public int getTeamNumber(final int index) {
        if (index < 0 || index >= mRecords.size()) return -1;
        return mRecords.get(index).mTeamNumber;
    }

    /**
     * Get punch/init time from 'index' element of mRecords list
     * (mRecords should be previously filtered with getPunchesAtStation).
     *
     * @param index Index in mRecords list array
     * @return Team punch unixtime for element with this index
     */
    public long getTeamTime(final int index) {
        if (index < 0 || index >= mRecords.size()) return -1;
        return mRecords.get(index).mPointTime;
    }

    /**
     * Get team members mask from 'index' element of mRecords list
     * (mRecords should be previously filtered with getPunchesAtStation).
     *
     * @param index Index in mRecords list array
     * @return Team mask for element with this index
     */
    public int getTeamMask(final int index) {
        if (index < 0 || index >= mRecords.size()) return -1;
        return mRecords.get(index).mTeamMask;
    }

    /**
     * Get chip init time from 'index' element of mRecords list
     * (mRecords should be previously filtered with getPunchesAtStation).
     *
     * @param index Index in mRecords list array
     * @return Chip initialization unixtime for element with this index
     */
    public long getInitTime(final int index) {
        if (index < 0 || index >= mRecords.size()) return -1;
        return mRecords.get(index).mInitTime;
    }

    /**
     * Get all punched control points from the chip obtained by connected station.
     *
     * @param teamNumber     Team number in the chip
     * @param initTime       Initialization time of the chip
     * @param stationNumber  Connected station number
     * @param stationMAC     Connected station BT MAC address
     * @param maxPointNumber Max possible point number in current distance
     * @return List of points numbers
     */
    public List<Integer> getChipPunches(final int teamNumber, final long initTime,
                                        final int stationNumber, final long stationMAC,
                                        final int maxPointNumber) {
        // flag all punched points in the list of all distance points
        final boolean[] punched = new boolean[maxPointNumber + 1];
        for (final Record record : mRecords) {
            if (record.mTeamNumber == teamNumber && record.mInitTime == initTime
                    && record.mStationNumber == stationNumber && record.mStationMAC == stationMAC
                    && record.mPointNumber <= maxPointNumber) {
                punched[record.mPointNumber] = true;
            }
        }
        // Build sorted list of punched points
        final List<Integer> pointNumbers = new ArrayList<>();
        for (int i = 1; i <= maxPointNumber; i++) {
            if (punched[i]) {
                pointNumbers.add(i);
            }
        }
        return pointNumbers;
    }

    /**
     * Get records at current control point and station
     * (local punches only, not records copied from chips).
     *
     * @param pointNumber Control point / station number
     * @param stationMAC  Station MAC as long
     * @return New Records object with list of filtered records
     */
    public Records getPunchesAtStation(final int pointNumber, final long stationMAC) {
        final List<Record> punches = new ArrayList<>();
        // Filter records
        for (final Record record : mRecords) {
            if (record.mPointNumber == pointNumber && record.mStationMAC == stationMAC
                    && record.mStationNumber == pointNumber) {
                // Find previous punch of this team (if any)
                int index = -1;
                for (int i = 0; i < punches.size(); i++) {
                    if (punches.get(i).mTeamNumber == record.mTeamNumber) {
                        index = i;
                        break;
                    }
                }
                if (index >= 0) {
                    final Record previous = punches.get(index);
                    // Replace previous punch of the same team with new punch
                    // if the time of new punch is greater then old
                    if (record.mPointTime > previous.mPointTime
                            || record.mPointTime == previous.mPointTime
                            && record.mStationTime > previous.mStationTime) {
                        punches.remove(index);
                        punches.add(record);
                    }
                } else {
                    // It is the first punch of this team, add it
                    punches.add(record);
                }
            }
        }
        // Sort the array of records
        Collections.sort(punches);
        // Create new Records object and copy records to it
        final Records punchesAtPoint = new Records(0);
        for (final Record record : punches) {
            punchesAtPoint.addRecord(record);
        }
        return punchesAtPoint;
    }

    /**
     * Check if some records were not sent to site yet.
     *
     * @return True if one or more records was not sent yet
     */
    public boolean hasUnsentRecords() {
        for (final Record record : mRecords) {
            if (record.getStatus() != Record.STATUS_SENT) return true;
        }
        return false;
    }

    /**
     * Get statistic for sent/unsent chip initializations and punches.
     *
     * @return Array of four integers
     */
    public List<Integer> getStatistic() {
        // Init counters
        final List<Integer> statistic = new ArrayList<>();
        int initAll = 0;
        int initSent = 0;
        int punchAll = 0;
        int punchSent = 0;
        // Find number of each type of records
        for (final Record record : mRecords) {
            if (record.getMode() == StationAPI.MODE_INIT_CHIPS) {
                initAll++;
                if (record.getStatus() == Record.STATUS_SENT) initSent++;
            } else {
                punchAll++;
                if (record.getStatus() == Record.STATUS_SENT) punchSent++;
            }
        }
        // Return all numbers as array
        statistic.add(initAll);
        statistic.add(initSent);
        statistic.add(punchAll);
        statistic.add(punchSent);
        return statistic;
    }

    /**
     * Add a record (loaded from local database) to the list.
     * Modifies class instance.
     *
     * @param record Sportiduino record to add
     */
    void addRecord(final Record record) {
        mRecords.add(record);
    }

    /**
     * Create new record from paired station and add it to list of records.
     * Modifies class instance.
     *
     * @param station     Station where the chip was initialized or punched
     * @param initTime    Chip initialization time
     * @param teamNumber  Team number written in the chip
     * @param teamMask    Team members mask written in the chip
     * @param pointNumber Punched point number (can differ from station number)
     * @param pointTime   Control point punch time
     */
    public void addRecord(final StationAPI station, final long initTime, final int teamNumber,
                          final int teamMask, final int pointNumber, final long pointTime) {
        mRecords.add(new Record(station.getMACasLong(), station.getStationTime(),
                station.getTimeDrift(), station.getNumber(), station.getMode(), initTime,
                teamNumber, teamMask, pointNumber, pointTime, Record.STATUS_NEW));
    }

    /**
     * Save all new (unsaved) records to local database.
     * Modifies class instance.
     *
     * @param database Database object from application thread
     * @return Empty string in case of success, SQL exception message in case of error
     */
    public String saveNewRecords(final Database database) {
        // Don't try to save anything if database opening has been failed
        if (database == null) return SUCCESS;
        // Find all unsaved records and put them in the list
        final List<Record> unsavedRecords = new ArrayList<>();
        for (final Record record : mRecords) {
            if (record.getStatus() == Record.STATUS_NEW) {
                unsavedRecords.add(record);
            }
        }
        if (unsavedRecords.isEmpty()) return SUCCESS;
        // Try to save this list in the database
        try {
            database.saveRecords(unsavedRecords);
        } catch (SQLiteException e) {
            return e.getMessage();
        }
        // flag all new records as saved
        for (int i = 0; i < mRecords.size(); i++) {
            final Record record = mRecords.get(i);
            if (record.getStatus() == Record.STATUS_NEW) {
                record.setStatus(Record.STATUS_SAVED);
                mRecords.set(i, record);
            }
        }
        return SUCCESS;
    }

    /**
     * Mark all unsent records as sent.
     * Modifies class instance.
     *
     * @param expectedUnsentN Expected number of unsent records
     * @return true if actual number is equal to expected
     */
    boolean markRecordsSent(final int expectedUnsentN) {
        // Get actual number of unsent records
        int actualUnsentN = 0;
        for (final Record record : mRecords) {
            if (record.getStatus() != Record.STATUS_SENT) actualUnsentN++;
        }
        // Do nothing if actual number of unsent records differ from expected
        if (expectedUnsentN != actualUnsentN) return false;
        // Data is consistent, mark all unsent records as sent
        for (int i = 0; i < mRecords.size(); i++) {
            final Record record = mRecords.get(i);
            if (record.getStatus() != Record.STATUS_SENT) {
                record.setStatus(Record.STATUS_SENT);
                mRecords.set(i, record);
            }
        }
        return true;
    }

    /**
     * Replace team mask in the team punch record at the station
     * or add it as a new record with old punch time
     * and new mask and station parameters.
     * Modifies class instance.
     *
     * @param teamNumber Team number
     * @param newMask    New team mask
     * @param station    Connected station to get point number and other parameters
     * @param database   Local database for saving changes in records
     * @param replace    True if replace old record, false if add it as new record
     * @return True if succeeded
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean updateTeamMask(final int teamNumber, final int newMask,
                                  final StationAPI station, final Database database,
                                  final boolean replace) {
        // Find the record for the last punch of this team at this control point
        final int pointNumber = station.getNumber();
        int last = -1;
        long pointTime = 0;
        long stationTime = 0;
        for (int i = 0; i < mRecords.size(); i++) {
            final Record record = mRecords.get(i);
            if (record.mTeamNumber == teamNumber && record.mPointNumber == pointNumber
                    && (record.mPointTime > pointTime
                    || record.mPointTime == pointTime && record.mStationTime > stationTime)) {
                last = i;
                pointTime = record.mPointTime;
                stationTime = record.mStationTime;
            }
        }
        // Return if the teams has not punched at the control point
        if (last < 0) return false;
        // Don't replace mask if it is the same
        //if (mRecords.get(last).mTeamMask == newMask) return false;
        // Create a copy of original record with new mask and new station parameters
        final Record oldRecord = mRecords.get(last);
        final Record newRecord = new Record(station.getMACasLong(),
                station.getStationTime(), station.getTimeDrift(), pointNumber, station.getMode(),
                oldRecord.mInitTime, teamNumber, newMask, pointNumber, oldRecord.mPointTime,
                Record.STATUS_NEW);
        // Add new record or replace old one with new
        if (replace) {
            // Just replace mask in the local copy of station memory
            mRecords.set(last, newRecord);
            return true;
        } else {
            // Add it to global list of records and save it in local db
            mRecords.add(newRecord);
            return SUCCESS.equals(this.saveNewRecords(database));
        }
    }

    /**
     * Loose merging of two lists of records with replacing of old punches with new.
     * Modifies class instance.
     *
     * @param newRecords List of records to add
     * @return True if some records were added or replaced
     */
    public boolean merge(final Records newRecords) {
        boolean dataChanged = false;
        for (final Record newRecord : newRecords.mRecords) {
            boolean isSameTeam = false;
            for (int i = 0; i < mRecords.size(); i++) {
                final Record record = mRecords.get(i);
                if (record.mTeamNumber == newRecord.mTeamNumber
                        && record.mPointNumber == newRecord.mPointNumber) {
                    // This team has punched at this control point before
                    isSameTeam = true;
                    if (record.mPointTime == newRecord.mPointTime
                            && record.mTeamMask == newRecord.mTeamMask) {
                        // It is the same punch, skip it
                        break;
                    }
                    if (record.mPointTime <= newRecord.mPointTime) {
                        // Team time and/or mask has been changed, replace old record with new
                        mRecords.set(i, newRecord);
                        dataChanged = true;
                        break;
                    }
                }
            }
            // If it was same/new punch of already seen team, do nothing
            if (isSameTeam) continue;
            // It is completely new record, add it to the list
            mRecords.add(newRecord);
            dataChanged = true;
        }
        return dataChanged;
    }

    /**
     * Strict joining of two lists of records,
     * records are not replaced, they are only added or skipped as full duplicates.
     * Modifies class instance.
     *
     * @param newRecords List of records to add
     * @return True if some records were added
     */
    public boolean join(final Records newRecords) {
        boolean dataChanged = false;
        for (final Record newRecord : newRecords.mRecords) {
            boolean isSameRecord = false;
            for (final Record record : this.mRecords) {
                if (record.mTeamNumber == newRecord.mTeamNumber
                        && record.mPointTime == newRecord.mPointTime
                        && record.mTeamMask == newRecord.mTeamMask
                        && record.mPointNumber == newRecord.mPointNumber
                        && record.mInitTime == newRecord.mInitTime
                        && record.mStationMAC == newRecord.mStationMAC
                        && record.mStationNumber == newRecord.mStationNumber
                        && record.mStationMode == newRecord.mStationMode) {
                    isSameRecord = true;
                    break;
                }
            }
            // Skip identical records
            if (isSameRecord) continue;
            mRecords.add(newRecord);
            dataChanged = true;
        }
        return dataChanged;
    }

    /**
     * Clear list of records.
     * Modifies class instance.
     */
    void clear() {
        mRecords.clear();
    }

    /**
     * Get number of records.
     *
     * @return Number of records
     */
    public int size() {
        return mRecords.size();
    }

    /**
     * Checks if the list of records contains a record for specific team and point.
     *
     * @param team  Team number to search
     * @param point Point number to search
     * @return true if a record for the team at the point has been found
     */
    public boolean contains(final int team, final int point) {
        for (final Record record : mRecords) {
            if (record.mTeamNumber == team && record.mPointNumber == point) return true;
        }
        return false;
    }

    /**
     * Sort list of records by their time in ascending order.
     */
    public void sort() {
        Collections.sort(mRecords);
    }
}
