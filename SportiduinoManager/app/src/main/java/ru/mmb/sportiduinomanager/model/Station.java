package ru.mmb.sportiduinomanager.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import ru.mmb.sportiduinomanager.R;

/**
 * Provides all communication with a Bluetooth station.
 */
public final class Station {
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
     * Internal messages inside station data exchange.
     */
    private static final String CALLER_INTERNAL = "SiMan Station";

    /**
     * Size of last teams buffer in station.
     */
    public static final int LAST_TEAMS_LEN = 10;
    /**
     * Max number of punches that we can get in one request from station.
     */
    public static final int MAX_PUNCH_COUNT = 61;
    /**
     * Size of chip UID in communication packet.
     */
    public static final int UID_SIZE = 8;
    /**
     * Timeout (im ms) while waiting for station response.
     */
    private static final int WAIT_TIMEOUT = 30_000;
    /**
     * Maximum size of communication packet.
     */
    private static final int MAX_PACKET_SIZE = 255;
    /**
     * Size of header in communication packet.
     */
    private static final int HEADER_SIZE = 6;
    /**
     * Protocol signature in all headers.
     */
    private static final byte HEADER_SIGNATURE = (byte) 0xFE;
    /**
     * Result of sending command to station: everything is ok.
     */
    private static final byte ALL_OK = 0;
    /**
     * Result of sending command to station: sending data failed.
     */
    private static final byte SEND_FAILED = 1;
    /**
     * Result of sending command to station: receiving response timeout.
     */
    private static final byte REC_TIMEOUT = 2;
    /**
     * Result of sending command to station: response has wrong format or damaged.
     */
    private static final byte REC_BAD_RESPONSE = 3;
    /**
     * Result of sending command to station: station sent an error.
     * of command execution
     */
    private static final byte REC_COMMAND_ERROR = 4;
    /**
     * Code of setMode station command.
     */
    private static final byte CMD_SET_MODE = (byte) 0x80;
    /**
     * Code of setTime station command.
     */
    private static final byte CMD_SET_TIME = (byte) 0x81;
    /**
     * Code of resetStation station command.
     */
    private static final byte CMD_RESET_STATION = (byte) 0x82;
    /**
     * Code of getStatus station command.
     */
    private static final byte CMD_GET_STATUS = (byte) 0x83;
    /**
     * Code of chipInit station command.
     */
    private static final byte CMD_INIT_CHIP = (byte) 0x84;
    /**
     * Code of getLastTeams station command.
     */
    private static final byte CMD_LAST_TEAMS = (byte) 0x85;
    /**
     * Code of getTeamRecord station command.
     */
    private static final byte CMD_TEAM_RECORD = (byte) 0x86;
    /**
     * Code of readCardPage station command.
     */
    private static final byte CMD_READ_CARD = (byte) 0x87;
    /**
     * Code of updateTeamMask station command.
     */
    private static final byte CMD_UPDATE_MASK = (byte) 0x88;
    /**
     * Code of readFlash station command.
     */
    private static final byte CMD_READ_FLASH = (byte) 0x8a;
    /**
     * Code of getConfig station command.
     */
    private static final byte CMD_GET_CONFIG = (byte) 0x8d;
    /**
     * Default UUID of station Bluetooth socket.
     */
    private static final UUID STATION_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Station Bluetooth whole object.
     */
    private final BluetoothDevice mDevice;
    /**
     * List of last teams which punched at the station.
     */
    private final List<Integer> mLastTeams;
    /**
     * Result of getTeamRecord call.
     */
    private final Records mTeamPunches;
    /**
     * Number of used pages in chips from getTeamRecord call.
     */
    private int mChipRecordsN;
    /**
     * Connected station Bluetooth socket.
     */
    private BluetoothSocket mSocket;
    /**
     * Code of last error of communication with station.
     */
    private int mLastError;
    /**
     * Android time at the start of command processing.
     */
    private long mStartTime;
    /**
     * Station local time at the end of getStatus/setTime.
     */
    private long mStationTime;
    /**
     * Time waiting for station while receiving station response.
     */
    private long mResponseTime;
    /**
     * Time difference in seconds between the station and Android.
     */
    private int mTimeDrift;
    /**
     * Time of last chip initialization written in a chip.
     */
    private long mLastInitTime;
    /**
     * Current station mode (0, 1 or 2).
     */
    private int mMode;
    /**
     * Configurable station number
     * (an control point number to work at or zero for chip initialization).
     */
    private int mNumber;
    /**
     * True when StationQuerying is allowed by activity to send commands to station.
     */
    private boolean mQueryingAllowed;
    /**
     * True when StationQuerying is in the middle of sending commands to station.
     */
    private boolean mQueryingActive;
    /**
     * Number of teams who checked in.
     */
    private int mChipsRegistered;
    /**
     * Time of last punch.
     */
    private long mLastPunchTime;
    /**
     * Station firmware version received from getConfig.
     */
    private int mFirmware;
    /**
     * Station battery voltage in Volts.
     */
    private float mVoltage;
    /**
     * Station battery voltage coefficient to convert to Volts from getStatus.
     */
    private float mVCoeff;
    /**
     * Station temperature received from getStatus.
     */
    private int mTemperature;
    /**
     * Loaded chip pages info.
     */
    private byte[] mChipInfo;

    /**
     * Create Station from Bluetooth scan.
     *
     * @param device Bluetooth device handler
     */
    public Station(final BluetoothDevice device) {
        mDevice = device;
        mLastError = 0;
        mTimeDrift = 0;
        mMode = 0;
        mNumber = 0;
        mQueryingAllowed = false;
        mQueryingActive = false;
        mChipsRegistered = 0;
        mLastPunchTime = 0;
        mFirmware = 0;
        mVoltage = 0;
        mVCoeff = 0.005_870f;
        mTemperature = 0;
        mLastTeams = new ArrayList<>();
        mTeamPunches = new Records(0);
        mChipRecordsN = 0;
        // Create client socket with default Bluetooth UUID
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(STATION_UUID);
        } catch (IOException ignored) {
            // Just let mSocket to stay equal null
        }
    }

    /**
     * Get station name + MAC.
     *
     * @return String with Bluetooth name and MAC address
     */
    public String getName() {
        return mDevice.getName() + " (" + mDevice.getAddress() + ")";
    }

    /**
     * Get station MAC.
     *
     * @return String with Bluetooth module MAC address
     */
    public String getAddress() {
        return mDevice.getAddress();
    }

    /**
     * Get station MAC as long integer.
     *
     * @return Bluetooth module MAC address as 8 bytes
     */
    public long getMACasLong() {
        final String hex = mDevice.getAddress().replace(":", "");
        return Long.parseLong(hex, 16);
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
     * Get time spent waiting while receiving station response for last command.
     *
     * @return Response time in ms
     */
    public long getResponseTime() {
        return mResponseTime;
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
     * Get configurable station number (a control point number to work at).
     *
     * @return Control point number, zero for chip initialization point
     */
    public int getNumber() {
        return mNumber;
    }

    /**
     * Check if StationQuerying is allowed by activity to send commands to station.
     *
     * @return True if allowed
     */
    public boolean isQueryingAllowed() {
        return mQueryingAllowed;
    }

    /**
     * Allow/disallow StationQuerying to send commands to station.
     *
     * @param isAllowed True for allowing to send commands.
     */
    public void setQueryingAllowed(final boolean isAllowed) {
        mQueryingAllowed = isAllowed;
    }

    /**
     * Flag the beginning/end of sending commands to station by StationQuerying.
     *
     * @param isActive True if querying is going to start
     */
    public void setQueryingActive(final boolean isActive) {
        mQueryingActive = isActive;
    }

    /**
     * Wait for StationQuerying cycle to finish (1.25s as hard limit).
     */
    public void waitForQuerying2Stop() {
        for (int i = 0; i < 50; i++) {
            if (!mQueryingActive) return;
            sleep();
        }
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
     * Get number of chips registered by station.
     *
     * @return Number of teams who checked in
     */
    public int getChipsRegistered() {
        return mChipsRegistered;
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
     * Get list of last teams punched at the station.
     *
     * @return Copy of mLastTeams array containing last teams numbers
     */
    public List<Integer> getLastTeams() {
        return new ArrayList<>(mLastTeams);
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
     * Get result of getTeamRecord call.
     *
     * @return List of all records read from team chip
     */
    public Records getTeamPunches() {
        return mTeamPunches;
    }

    /**
     * Get number of used pages in team chip from getTeamRecord call.
     *
     * @return Total number of chip pages to read from station flash
     */
    public int getChipRecordsN() {
        return mChipRecordsN;
    }

    /**
     * Get data from chip fetched in fetchChipInfo.
     *
     * @return response byte array
     */
    public byte[] getChipInfo() {
        if (mChipInfo == null || mChipInfo.length == 0) return null;
        return Arrays.copyOf(mChipInfo, mChipInfo.length);
    }

    /**
     * Connect to station Bluetooth adapter.
     *
     * @return True in case of success
     */
    public boolean connect() {
        // Check if BT socket was not created during station initialization
        if (mSocket == null) return false;
        // check if already connected
        if (mSocket.isConnected()) return true;
        // Try to connect to the remote device through the socket.
        // This call blocks until it succeeds or throws an exception.
        try {
            mSocket.connect();
            return true;
        } catch (IOException connectException) {
            disconnect();
            return false;
        }
    }

    /**
     * Disconnect from station by closing BT input/output streams and BT socket.
     */
    public void disconnect() {
        // return immediately if the socket was never connected
        if (mSocket == null) return;
        // Close socket
        try {
            mSocket.close();
        } catch (IOException ignored) {
            // Don't care if we had problems -
            // a user just stopped working with this station
        }
    }

    /**
     * Write command to station input stream.
     *
     * @param command Command payload without start and stop bytes
     * @return True if the command was sent
     */
    private boolean send(final byte[] command) {
        final int len = command.length;
        if (len == 0 || len > MAX_PACKET_SIZE) return false;
        // prepare output buffer
        byte[] buffer = new byte[len + HEADER_SIZE];
        buffer[0] = HEADER_SIGNATURE;
        buffer[1] = HEADER_SIGNATURE;
        buffer[2] = HEADER_SIGNATURE;
        buffer[3] = (byte) (mNumber & 0xFF);
        buffer[4] = (byte) ((len - 1) & 0xFF);
        System.arraycopy(command, 0, buffer, 5, len);
        buffer[len + HEADER_SIZE - 1] = crc8(buffer, len + HEADER_SIZE - 1);
        // send output buffer to station Bluetooth socket
        try {
            @SuppressWarnings("PMD.CloseResource")
            final OutputStream output = mSocket.getOutputStream();
            output.write(buffer);
            output.flush();
        } catch (IOException e) {
            Log.d(CALLER_INTERNAL, "send: " + e.getMessage());
            // station got disconnected
            disconnect();
            return false;
        }
        return true;
    }

    /**
     * Sleep while waiting for station response to arrive.
     */
    private void sleep() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Read raw data in PACKET_SIZE-byte blocks from station output stream.
     *
     * @return Station response or null in case of timeout or exception
     */
    private byte[] receive() {
        byte[] response = new byte[0];
        // try to open Bluetooth socket input stream
        // read from station Bluetooth socket
        final byte[] buffer = new byte[MAX_PACKET_SIZE];
        try {
            @SuppressWarnings("PMD.CloseResource")
            final InputStream input = mSocket.getInputStream();
            while (System.currentTimeMillis() - mStartTime < WAIT_TIMEOUT) {
                // wait for data to appear in input stream
                if (input.available() == 0) {
                    sleep();
                    continue;
                }
                // read data into MAX_PACKET_SIZE byte buffer
                final int newLen = input.read(buffer);
                if (newLen == 0) continue;
                // add received bytes (PACKET_SIZE or less) to response array
                final int oldLen = response.length;
                if (oldLen == 0) {
                    response = new byte[newLen];
                    System.arraycopy(buffer, 0, response, 0, newLen);
                } else {
                    final byte[] temp = new byte[oldLen + newLen];
                    System.arraycopy(response, 0, temp, 0, oldLen);
                    System.arraycopy(buffer, 0, temp, oldLen, newLen);
                    response = temp;
                }
                // stop waiting for more data if we got whole packet
                if (response.length >= HEADER_SIZE && response.length
                        >= ((response[HEADER_SIZE - 2] & 0xFF) + HEADER_SIZE + 1)) {
                    return response;
                }
            }
            return response;
        } catch (IOException e) {
            Log.d(CALLER_INTERNAL, "receive: " + e.getMessage());
            // station got disconnected
            disconnect();
            return response;
        }
    }

    /**
     * Send command to station, receive response and make response integrity checks.
     *
     * @param sendBuffer Actual command payload without starting and ending bytes
     * @return Byte array with station response, first byte contains error code
     */
    private byte[] runCommand(final byte[] sendBuffer) {
        // reconnect (just in case and send the command
        if (!connect()) return new byte[]{SEND_FAILED};
        if (!send(sendBuffer)) return new byte[]{SEND_FAILED};
        // get station response
        final byte[] receiveBuffer = receive();
        final int len = receiveBuffer.length;
        if (len == 0) return new byte[]{REC_TIMEOUT};
        // check if response has at minimum 1 byte payload
        if (len < HEADER_SIZE) return new byte[]{REC_BAD_RESPONSE};
        // check buffer length
        if ((receiveBuffer[HEADER_SIZE - 2] & 0xFF) != len - HEADER_SIZE - 1) {
            return new byte[]{REC_BAD_RESPONSE};
        }
        // check header
        if (receiveBuffer[0] != HEADER_SIGNATURE || receiveBuffer[1] != HEADER_SIGNATURE
                || receiveBuffer[2] != HEADER_SIGNATURE) {
            return new byte[]{REC_BAD_RESPONSE};
        }
        // check station number
        if (sendBuffer[0] != CMD_GET_STATUS && sendBuffer[0] != CMD_GET_CONFIG
                && sendBuffer[0] != CMD_RESET_STATION
                && (receiveBuffer[3] & 0xFF) != mNumber) {
            return new byte[]{REC_BAD_RESPONSE};
        }
        // update station number for getStatus command
        if (sendBuffer[0] == CMD_GET_STATUS) {
            mNumber = receiveBuffer[3] & 0xFF;
        }
        // check crc
        if (receiveBuffer[len - 1] != crc8(receiveBuffer, len - 1)) {
            return new byte[]{REC_BAD_RESPONSE};
        }
        // check if command code received is equal to command code sent
        if (receiveBuffer[HEADER_SIZE - 1] != sendBuffer[0] + 0x10) {
            return new byte[]{REC_BAD_RESPONSE};
        }
        // check command execution code (it should be present and equal to zero)
        if (receiveBuffer[HEADER_SIZE] != 0) {
            return new byte[]{(byte) (REC_COMMAND_ERROR + receiveBuffer[HEADER_SIZE])};
        }
        // copy payload to response buffer
        final byte[] response = new byte[len - HEADER_SIZE - 1];
        response[0] = ALL_OK;
        System.arraycopy(receiveBuffer, HEADER_SIZE + 1, response, 1, len - HEADER_SIZE - 2);
        return response;
    }

    /**
     * Call runCommand (which performs communication with a station),
     * receive response, check it, set mLastError in case of error and fill
     * responseContent with actual station response.
     *
     * @param commandContent  Command payload sent to station
     * @param responseContent Station response without service bytes
     * @param caller          Name of caller activity for Logcat
     * @return True if there was no communication or command execution errors
     */
    private boolean command(final byte[] commandContent, final byte[] responseContent,
                            final String caller) {
        // Save time at the beginning of command processing
        mStartTime = System.currentTimeMillis();
        // TODO: remove debug output
        Log.d(caller, " command " + String.format("%02x", commandContent[0]) + " started  at " + mStartTime);
        // Communicate with the station
        final byte[] rawResponse = runCommand(commandContent);
        // Compute execution time
        final long now = System.currentTimeMillis();
        mResponseTime = now - mStartTime;
        // TODO: remove debug output
        Log.d(caller, " command " + String.format("%02x", commandContent[0]) + " finished  at " + now);
        // Check for command execution errors and response parsing errors
        if (rawResponse[0] != ALL_OK) {
            switch (rawResponse[0]) {
                case SEND_FAILED:
                    mLastError = R.string.err_bt_send_failed;
                    return false;
                case REC_TIMEOUT:
                    mLastError = R.string.err_bt_receive_timeout;
                    return false;
                case REC_BAD_RESPONSE:
                    mLastError = R.string.err_bt_receive_bad_response;
                    return false;
                case REC_COMMAND_ERROR + 1:
                    mLastError = R.string.err_station_wrong_number;
                    return false;
                case REC_COMMAND_ERROR + 2:
                    mLastError = R.string.err_station_read;
                    return false;
                case REC_COMMAND_ERROR + 3:
                    mLastError = R.string.err_station_write;
                    return false;
                case REC_COMMAND_ERROR + 4:
                    mLastError = R.string.err_station_init_chip;
                    return false;
                case REC_COMMAND_ERROR + 5:
                    mLastError = R.string.err_station_bad_chip;
                    return false;
                case REC_COMMAND_ERROR + 6:
                    mLastError = R.string.err_station_no_chip;
                    return false;
                case REC_COMMAND_ERROR + 7:
                    mLastError = R.string.err_station_buffer_overflow;
                    return false;
                case REC_COMMAND_ERROR + 8:
                    mLastError = R.string.err_station_reset_impossible;
                    return false;
                case REC_COMMAND_ERROR + 9:
                    mLastError = R.string.err_station_incorrect_uid;
                    return false;
                case REC_COMMAND_ERROR + 10:
                    mLastError = R.string.err_station_wrong_team;
                    return false;
                case REC_COMMAND_ERROR + 11:
                    mLastError = R.string.err_station_no_data;
                    return false;
                case REC_COMMAND_ERROR + 12:
                    mLastError = R.string.err_station_bad_command;
                    return false;
                case REC_COMMAND_ERROR + 13:
                    mLastError = R.string.err_station_erase_flash;
                    return false;
                case REC_COMMAND_ERROR + 14:
                    mLastError = R.string.err_station_bad_chip_type;
                    return false;
                default:
                    mLastError = R.string.err_station_unknown;
                    return false;
            }
        }
        // Check if the actual response length is equal to expected length
        if (rawResponse.length != responseContent.length + 1) {
            mLastError = R.string.err_bt_response_wrong_length;
            return false;
        }
        // Everything is OK, copy station response content
        System.arraycopy(rawResponse, 1, responseContent, 0, responseContent.length);
        return true;
    }

    /**
     * Get the last communication error and reset it to zero.
     *
     * @param resetError Reset error to "no error" after returning its value
     * @return Code of last error or zero (if no errors occurred)
     */
    public int getLastError(final boolean resetError) {
        final int error = mLastError;
        if (resetError) {
            mLastError = 0;
        }
        return error;
    }

    /**
     * Compute CRC8 checksum.
     *
     * @param array Array of bytes
     * @param end   ending position for computing crc
     * @return CRC8
     */
    private byte crc8(final byte[] array, final int end) {
        byte crc = 0x00;
        for (int i = 2; i < end; i++) {
            byte extract = array[i];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) ((sum & 0xFF) & 0x01);
                crc = (byte) ((crc & 0xFF) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xFF) ^ 0x8C);
                }
                extract = (byte) ((extract & 0xFF) >>> 1);
            }
        }
        return (byte) (crc & 0xFF);
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
        if (mNumber == 0 && mode != MODE_INIT_CHIPS) {
            mLastError = R.string.err_init_wrong_mode;
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
            mLastError = R.string.err_station_wrong_number;
            return false;
        }
        // Prepare command payload
        byte[] commandData = new byte[8];
        commandData[0] = CMD_RESET_STATION;
        long2ByteArray(mChipsRegistered, commandData, 1, 2);
        long2ByteArray((int) mLastPunchTime, commandData, 3, 4);
        commandData[7] = (byte) number;
        // Send it to station
        final byte[] response = new byte[0];
        if (!command(commandData, response, caller)) return false;
        // Update station number and mode in class object
        mNumber = number;
        mMode = MODE_INIT_CHIPS;
        // Forget all teams and their punches which have been occurred before reset
        mLastTeams.clear();
        mTeamPunches.clear();
        mChipRecordsN = 0;
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
        // Get number of chips registered by station
        mChipsRegistered = (int) byteArray2Long(response, 4, 5);
        // Get last chips registration time
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
        mTimeDrift = (int) (mStationTime - mStartTime);
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
    public boolean fetchTeamRecord(final int teamNumber, final String caller) {
        // Prepare command payload
        byte[] commandData = new byte[3];
        commandData[0] = CMD_TEAM_RECORD;
        long2ByteArray(teamNumber, commandData, 1, 2);
        // Send command to station
        final byte[] response = new byte[13];
        mTeamPunches.clear();
        mChipRecordsN = 0;
        if (!command(commandData, response, caller)) return false;
        // Parse response
        final int checkTeamNumber = (int) byteArray2Long(response, 0, 1);
        if (checkTeamNumber != teamNumber) {
            mLastError = R.string.err_station_team_changed;
            return false;
        }
        final long initTime = byteArray2Long(response, 2, 5);
        final int teamMask = (int) byteArray2Long(response, 6, 7);
        final long teamTime = byteArray2Long(response, 8, 11);
        mTeamPunches.addRecord(this, initTime, teamNumber, teamMask, mNumber, teamTime);
        mChipRecordsN = response[12] & 0xFF;
        // Check if we have a valid number of punches in copy of the chip in flash memory
        if (mChipRecordsN <= 8 || mChipRecordsN >= 0xFF) {
            mChipRecordsN = 0;
            mLastError = R.string.err_station_flash_empty;
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
     * @return True if succeeded, fills mTeamPunches with punches as Record instances
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean fetchTeamPunches(final int teamNumber, final long initTime, final int teamMask, final int fromPunch,
                                    final int count, final String caller) {
        if (count <= 0 || count > MAX_PUNCH_COUNT) {
            mLastError = R.string.err_station_buffer_overflow;
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
        mTeamPunches.clear();
        mChipRecordsN = 0;
        if (!command(commandData, response, caller)) return false;
        // Check that read address in response is equal to read address in command
        if (startAddress != byteArray2Long(response, 0, 3)) {
            mLastError = R.string.err_station_address_changed;
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
            mTeamPunches.addRecord(this, initTime, teamNumber, teamMask, pointNumber, pointTime);
        }
        // Check number of actually read punches
        if (mTeamPunches.size() == count) {
            return true;
        } else {
            mLastError = R.string.err_station_flash_empty;
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
