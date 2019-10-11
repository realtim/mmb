package ru.mmb.sportiduinomanager.model;

import android.bluetooth.BluetoothDevice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import ru.mmb.sportiduinomanager.R;

/**
 * Provides all communication with a Bluetooth station.
 */
public final class StationAPI extends StationRaw {
    /**
     * Station mode for chips initialization.
     */
    public static final int MODE_INIT_CHIPS = 0;
    /**
     * Station mode for an ordinary control point.
     */
    public static final int MODE_OTHER_POINT = 1;
    /**
     * Station mode for control point at distance segment end.
     */
    public static final int MODE_FINISH_POINT = 2;
    /**
     * Size of last teams buffer in station.
     */
    public static final int LAST_TEAMS_LEN = 10;
    /**
     * Max number of punches that we can get in one request from station.
     */
    public static final int MAX_PUNCH_COUNT = 61;
    /**
     * Caller of station command is BluetoothActivity.
     */
    public static final String CALLER_BLUETOOTH = "SiMan Bluetooth";
    /**
     * Caller of station command is ControlPointActivity.
     */
    public static final String CALLER_CP = "SiMan ControlPoint";
    /**
     * Caller of station command is StationQuerying task.
     */
    public static final String CALLER_QUERYING = "SiMan StationQuery";
    /**
     * Caller of station command is ChipInfoActivity.
     */
    public static final String CALLER_CHIP_INFO = "SiMan ChipInfo";
    /**
     * Caller of station command is ChipInitTask.
     */
    public static final String CALLER_CHIP_INIT = "SiMan ChipInit";
    /**
     * Caller of station command is StationResetTask.
     */
    public static final String CALLER_RESET = "SiMan StationReset";

    /**
     * List of last teams (up to 10) which punched at the station.
     */
    private final List<Integer> mLastTeams;
    /**
     * Chip records received from fetchTeamHeader/fetchTeamPunches methods.
     */
    private final Records mRecords;
    /**
     * Current station mode (0, 1 or 2).
     */
    private int mMode;
    /**
     * Station local time at the end of getStatus/setTime.
     */
    private long mStationTime;
    /**
     * Time difference in seconds between the station and Android.
     */
    private int mTimeDrift;
    /**
     * Time of last chip initialization written in a chip.
     */
    private long mLastInitTime;
    /**
     * Time of last punch at the station.
     */
    private long mLastPunchTime;
    /**
     * Number of teams which already punched at the station.
     */
    private int mTeamsPunched;
    /**
     * Number of nonempty records in chip from fetchTeamHeader method.
     */
    private int mChipRecordsN;
    /**
     * Chip content for ChipInfo activity.
     */
    private byte[] mChipInfo;
    /**
     * Station firmware version received from getConfig.
     */
    private int mFirmware;
    /**
     * Station battery voltage in Volts.
     */
    private float mVoltage;
    /**
     * Station temperature received from getStatus.
     */
    private int mTemperature;
    /**
     * Station battery voltage coefficient to convert to Volts from getStatus.
     */
    private float mVCoeff;


    /**
     * Create Station from Bluetooth scan.
     *
     * @param device Bluetooth device handler
     */
    public StationAPI(final BluetoothDevice device) {
        super(device);
        mLastTeams = new ArrayList<>();
        mRecords = new Records(0);
        mMode = 0;
        mStationTime = 0;
        mTimeDrift = 0;
        mLastInitTime = 0;
        mLastPunchTime = 0;
        mTeamsPunched = 0;
        mChipRecordsN = 0;
        mFirmware = 0;
        mVoltage = 0;
        mTemperature = 0;
        mVCoeff = 0.005_870f;
    }

    /**
     * Get list of last teams punched at the station.
     *
     * @return Copy of mLastTeams array containing last teams numbers
     */
    public List<Integer> getLastTeams() {
        return new ArrayList<>(mLastTeams);
    }

    /**
     * Get chip records received from fetchTeamHeader/fetchTeamPunches methods.
     *
     * @return Single record from fetchTeamHeader or list from fetchTeamPunches
     */
    public Records getRecords() {
        return mRecords;
    }

    /**
     * Get station mode.
     *
     * @return Mode code (0, 1 or 2)
     */
    public int getMode() {
        return mMode;
    }

    /**
     * Get station time at the end of last command processing.
     *
     * @return Time as unixtime
     */
    public long getStationTime() {
        return mStationTime;
    }

    /**
     * Get station time drift.
     *
     * @return Time difference in seconds between the station and Android
     */
    public int getTimeDrift() {
        return mTimeDrift;
    }

    /**
     * Get last initialization time written in a chip.
     *
     * @return Time as unixtime
     */
    public long getLastInitTime() {
        return mLastInitTime;
    }

    /**
     * Get the time of last punch.
     *
     * @return Unixtime of last punch
     */
    public long getLastPunchTime() {
        return mLastPunchTime;
    }

    /**
     * Get number of teams which already punched at the station.
     *
     * @return Number of punched teams
     */
    public int getTeamsPunched() {
        return mTeamsPunched;
    }

    /**
     * Get number of nonempty records in team chip from getTeamRecord call.
     *
     * @return Total number of chip records to read from station flash
     */
    public int getChipRecordsN() {
        return mChipRecordsN;
    }

    /**
     * Get chip content for ChipInfo activity.
     *
     * @return response byte array
     */
    public byte[] getChipInfo() {
        if (mChipInfo == null || mChipInfo.length == 0) return null;
        return Arrays.copyOf(mChipInfo, mChipInfo.length);
    }

    /**
     * Get station firmware version.
     *
     * @return Firmware version
     */
    public int getFirmware() {
        return mFirmware;
    }

    /**
     * Get station battery voltage.
     *
     * @return Battery voltage in Volts
     */
    public float getVoltage() {
        return mVoltage;
    }

    /**
     * Get station temperature.
     *
     * @return Station temperature in Celsius degrees
     */
    public int getTemperature() {
        return mTemperature;
    }

    /**
     * Convert byte array section to int.
     *
     * @param array Array of bytes
     * @param start Starting position of byte sequence which will be converted to int
     * @param end   Ending position of byte sequence
     * @return Long representation of byte sequence
     */
    private long byteArray2Long(final byte[] array, final int start, final int end) {
        long result = 0;
        for (int i = start; i <= end; i++) {
            result = result | (array[i] & 0xFF) << ((end - i) * 8);
        }
        return result;
    }

    /**
     * Copy first count bytes from int value to the section of byte array.
     *
     * @param value    Long value to copy
     * @param array    Target byte array
     * @param starting Starting position in byte array to copy long value
     * @param count    Number of bytes to copy
     */
    private void long2ByteArray(final long value, final byte[] array, final int starting,
                                final int count) {
        byte[] converted = new byte[count];
        for (int i = 0; i < count; i++) {
            converted[count - 1 - i] = (byte) ((value >> 8 * i) & 0xFF);
        }
        System.arraycopy(converted, 0, array, starting, count);
    }

    /**
     * Set station mode and number.
     *
     * @param mode   New station mode (chip initialization, ordinary or finish point)
     * @param caller Name of caller activity for Logcat
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean newMode(final int mode, final String caller) {
        // Zero number is only for chip initialization
        if (getNumber() == 0 && mode != MODE_INIT_CHIPS) {
            setLastError(R.string.err_init_wrong_mode);
            return false;
        }
        final byte[] response = new byte[0];
        if (command(new byte[]{CMD_SET_MODE, (byte) mode}, response, caller)) {
            mMode = mode;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set station clock to Android local time converted to UTC timezone.
     *
     * @param caller Name of caller activity for Logcat
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean syncTime(final String caller) {
        byte[] commandData = new byte[7];
        commandData[0] = CMD_SET_TIME;
        // Get current UTC time
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        commandData[1] = (byte) (calendar.get(Calendar.YEAR) - 2000);
        commandData[2] = (byte) (calendar.get(Calendar.MONTH) + 1);
        commandData[3] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        commandData[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        commandData[5] = (byte) calendar.get(Calendar.MINUTE);
        commandData[6] = (byte) (calendar.get(Calendar.SECOND));
        // Send it to station
        final byte[] response = new byte[4];
        if (!command(commandData, response, caller)) return false;
        // Get new station time
        mStationTime = byteArray2Long(response, 0, 3);
        mTimeDrift = (int) (mStationTime - System.currentTimeMillis() / 1000L);
        return true;
    }

    /**
     * Reset station by giving it new number and erasing all data in it.
     *
     * @param number New station number
     * @param caller Name of caller activity for Logcat
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean resetStation(final int number, final String caller) {
        // Don't allow 0xFF station number
        if (number < 0 || number >= 0xFF) {
            setLastError(R.string.err_station_wrong_number);
            return false;
        }
        // Prepare command payload
        byte[] commandData = new byte[8];
        commandData[0] = CMD_RESET_STATION;
        long2ByteArray(mTeamsPunched, commandData, 1, 2);
        long2ByteArray((int) mLastPunchTime, commandData, 3, 4);
        commandData[7] = (byte) number;
        // Send it to station
        final byte[] response = new byte[0];
        if (!command(commandData, response, caller)) return false;
        // Update station number and mode in class object
        setNumber(number);
        mMode = MODE_INIT_CHIPS;
        // Forget all teams and their punches which have been occurred before reset
        mChipRecordsN = 0;
        mRecords.clear();
        mLastTeams.clear();
        return true;
    }

    /**
     * Get station local time, mode, number, etc.
     *
     * @param caller Name of caller activity for Logcat
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean fetchStatus(final String caller) {
        // Get response from station
        final byte[] response = new byte[14];
        if (!command(new byte[]{CMD_GET_STATUS}, response, caller)) return false;
        // Get station time
        mStationTime = byteArray2Long(response, 0, 3);
        mTimeDrift = (int) (mStationTime - System.currentTimeMillis() / 1000L);
        // Get number of teams already punched at the station
        mTeamsPunched = (int) byteArray2Long(response, 4, 5);
        // Get last chips punch time
        mLastPunchTime = byteArray2Long(response, 6, 9);
        // Get station battery voltage in "parrots"
        final int voltage = (int) byteArray2Long(response, 10, 11);
        // Convert value to Volts
        mVoltage = voltage * mVCoeff;
        // Get station temperature
        mTemperature = (int) byteArray2Long(response, 12, 13);
        return true;
    }

    /**
     * Init a chip with team number and members mask.
     *
     * @param teamNumber Team number
     * @param teamMask   Mask of team members presence
     * @param caller     Name of caller activity for Logcat
     * @return True if succeeded
     */
    public boolean initChip(final int teamNumber, final int teamMask, final String caller) {
        // Note: last 4 byte are reserved and equal to zero now
        byte[] commandData = new byte[5];
        commandData[0] = CMD_INIT_CHIP;
        // Prepare command payload
        long2ByteArray(teamNumber, commandData, 1, 2);
        long2ByteArray(teamMask, commandData, 3, 2);
        // Send command to station
        final byte[] response = new byte[12];
        if (!command(commandData, response, caller)) return false;
        // Get init time from station response
        mLastInitTime = byteArray2Long(response, 0, 3);
        // Update station time and drift
        mStationTime = mLastInitTime;
        mTimeDrift = (int) (mStationTime - getStartTime());
        // Chip UID (bytes 4-11) is ignored right now
        return true;
    }

    /**
     * Get list of last LAST_TEAMS_LEN teams which has been punched at the station.
     *
     * @param caller Name of caller activity for Logcat
     * @return True if succeeded
     */
    public boolean fetchLastTeams(final String caller) {
        // Get response from station
        final byte[] response = new byte[LAST_TEAMS_LEN * 2];
        if (!command(new byte[]{CMD_LAST_TEAMS}, response, caller)) return false;
        mLastTeams.clear();
        for (int i = 0; i < LAST_TEAMS_LEN; i++) {
            final int teamNumber = (int) byteArray2Long(response, i * 2, i * 2 + 1);
            if (teamNumber > 0 && !mLastTeams.contains(teamNumber)) mLastTeams.add(teamNumber);
        }
        return true;
    }

    /**
     * Get info about team with teamNumber number punched at the station.
     *
     * @param teamNumber Number of team to fetch
     * @param caller     Name of caller activity for Logcat
     * @return True if succeeded
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean fetchTeamHeader(final int teamNumber, final String caller) {
        // Prepare command payload
        byte[] commandData = new byte[3];
        commandData[0] = CMD_TEAM_RECORD;
        long2ByteArray(teamNumber, commandData, 1, 2);
        // Send command to station
        final byte[] response = new byte[13];
        mChipRecordsN = 0;
        mRecords.clear();
        if (!command(commandData, response, caller)) return false;
        // Parse response
        final int checkTeamNumber = (int) byteArray2Long(response, 0, 1);
        if (checkTeamNumber != teamNumber) {
            setLastError(R.string.err_station_team_changed);
            return false;
        }
        final long initTime = byteArray2Long(response, 2, 5);
        final int teamMask = (int) byteArray2Long(response, 6, 7);
        final long teamTime = byteArray2Long(response, 8, 11);
        mChipRecordsN = response[12] & 0xFF;
        // Save team punch at our station as single record in mRecords
        mRecords.addRecord(this, initTime, teamNumber, teamMask, getNumber(), teamTime);
        // Check if we have a valid number of punches in copy of the chip in flash memory
        if (mChipRecordsN <= 8 || mChipRecordsN >= 0xFF) {
            mChipRecordsN = 0;
            setLastError(R.string.err_station_flash_empty);
            return false;
        }
        mChipRecordsN -= 8;
        return true;
    }

    /**
     * Read chip information from 0 to 20 pages to mChipInfo byte array.
     *
     * @param pagesInRequest must be 20
     * @param requestsCount  multiplier by 20 to get reasonable pages count
     * @param caller         Name of caller activity for Logcat
     * @return true if succeeded
     */
    public boolean readCardPage(final byte pagesInRequest, final int requestsCount, final String caller) {
        // TODO: rewrite function for new API
        mChipInfo = new byte[]{};
        final byte[] concatResponse = new byte[UID_SIZE + (pagesInRequest * requestsCount + 1) * 5];
        for (int i = 0; i < requestsCount; i++) {
            final byte pageFrom = (byte) (i * pagesInRequest);
            final byte pageTo = (byte) (pageFrom + pagesInRequest);
            // Prepare command payload
            byte[] commandData = new byte[3];
            commandData[0] = CMD_READ_CARD;
            commandData[1] = pageFrom;
            commandData[2] = pageTo;
            final int expectedSize = UID_SIZE + (pageTo - pageFrom + 1) * 5;
            // Send command to station
            final byte[] response = new byte[expectedSize];
            if (!command(commandData, response, caller)) return false;
            if (pageFrom == 0) {
                System.arraycopy(response, 0, concatResponse, 0, expectedSize);
            } else {
                System.arraycopy(response, UID_SIZE, concatResponse, UID_SIZE + pageFrom * 5, expectedSize - UID_SIZE);
            }
        }
        mChipInfo = concatResponse;
        return true;
    }

    /**
     * Update team mask in station.
     *
     * @param teamNumber Number of team to update
     * @param initTime   Chip init time
     *                   (along with team number it is the primary key of a chip)
     * @param teamMask   New team members mask
     * @param caller     Name of caller activity for Logcat
     * @return true if succeeded
     */
    public boolean updateTeamMask(final int teamNumber, final long initTime, final int teamMask, final String caller) {
        // Prepare command payload
        byte[] commandData = new byte[9];
        commandData[0] = CMD_UPDATE_MASK;
        long2ByteArray(teamNumber, commandData, 1, 2);
        long2ByteArray(initTime, commandData, 3, 4);
        long2ByteArray(teamMask, commandData, 7, 2);
        // Send command to station
        final byte[] response = new byte[0];
        return command(commandData, response, caller);
    }

    /**
     * Get some punches from a copy of a chip in station flash memory.
     *
     * @param teamNumber Team number
     * @param initTime   Chip init time (for creation of new punch records)
     * @param teamMask   Chip team mask (for creation of new punch records)
     * @param fromPunch  Starting position in the list of punches
     * @param count      Number of punches to read
     * @param caller     Name of caller activity for Logcat
     * @return True if succeeded, fills mRecords with punches as Record instances
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean fetchTeamPunches(final int teamNumber, final long initTime, final int teamMask, final int fromPunch,
                                    final int count, final String caller) {
        if (count <= 0 || count > MAX_PUNCH_COUNT) {
            setLastError(R.string.err_station_buffer_overflow);
            return false;
        }
        // Prepare command payload
        byte[] commandData = new byte[6];
        commandData[0] = CMD_READ_FLASH;
        final long punchesZone = teamNumber * 1024L + 48L;
        final long startAddress = punchesZone + fromPunch * 4L;
        long2ByteArray(startAddress, commandData, 1, 4);
        commandData[5] = (byte) ((count * 4) & 0xFF);
        // Send command to station
        final byte[] response = new byte[4 + count * 4];
        mChipRecordsN = 0;
        mRecords.clear();
        if (!command(commandData, response, caller)) return false;
        // Check that read address in response is equal to read address in command
        if (startAddress != byteArray2Long(response, 0, 3)) {
            setLastError(R.string.err_station_address_changed);
            return false;
        }
        // Get first byte of current time
        final long timeCorrection = mStationTime & 0xFF000000;
        // Add new records
        for (int i = 0; i < count; i++) {
            final int pointNumber = response[4 + i * 4] & 0xFF;
            final long pointTime =
                    byteArray2Long(response, 5 + i * 4, 7 + i * 4) + timeCorrection;
            // Check if the record is non-empty
            if (pointNumber == 0xFF && (pointTime - timeCorrection) == 0x00FFFFFF) break;
            mRecords.addRecord(this, initTime, teamNumber, teamMask, pointNumber, pointTime);
        }
        // Check number of actually read punches
        if (mRecords.size() == count) {
            return true;
        } else {
            setLastError(R.string.err_station_flash_empty);
            return false;
        }
    }

    /**
     * Get station firmware version and configuration.
     *
     * @param caller Name of caller activity for Logcat
     * @return True if succeeded
     */
    public boolean fetchConfig(final String caller) {
        // Get response from station
        final byte[] response = new byte[20];
        if (!command(new byte[]{CMD_GET_CONFIG}, response, caller)) return false;
        // Get station firmware
        mFirmware = response[0];
        // Get station mode
        mMode = response[1] & 0xFF;
        // Get voltage coefficient
        mVCoeff = ByteBuffer.wrap(response, 7, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        // Ignore all other parameters
        return true;
    }
}
