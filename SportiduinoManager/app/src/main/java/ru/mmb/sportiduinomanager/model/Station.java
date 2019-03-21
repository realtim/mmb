package ru.mmb.sportiduinomanager.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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
    public static final byte MODE_INIT_CHIPS = 0;
    /**
     * Station mode for an ordinary active point.
     */
    public static final byte MODE_OTHER_POINT = 1;
    /**
     * Station mode for active point at distance segment end.
     */
    public static final byte MODE_FINISH_POINT = 2;

    /**
     * Timeout (im ms) while waiting for station response.
     */
    private static final int WAIT_TIMEOUT = 60_000;

    /**
     * Size of send/receive packets for station communications.
     */
    private static final int PACKET_SIZE = 32;

    /**
     * Protocol signature in all headers.
     */
    private static final byte HEADER_SIGNATURE = (byte) 0xFE;

    /**
     * Result of sending command to station: everything is ok.
     */
    private static final byte COMMAND_OK = 0;
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
     * Default UUID of station Bluetooth socket.
     */
    private static final UUID STATION_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Station Bluetooth whole object.
     */
    private final BluetoothDevice mDevice;

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
     * Station local time at the end of command processing.
     */
    private int mStationTime;

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
    private int mLastInitTime;

    /**
     * Current station mode (0, 1 or 2).
     */
    private byte mMode;

    /**
     * Configurable station number
     * (an active point number to work at or zero for chip initialization).
     */
    private byte mNumber;

    /**
     * Number of teams who checked in.
     */
    private int mChipsRegistered;

    /**
     * Text representation of time of last chip registration in local timezone.
     */
    private int mLastChipTime;

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
        mChipsRegistered = 0;
        mLastChipTime = 0;
        // Create client socket with default Bluetooth UUID
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(STATION_UUID);
        } catch (IOException e) {
            mSocket = null;
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
    long getMACasLong() {
        final String hex = mDevice.getAddress().replace(":", "");
        return Long.parseLong(hex, 16);
    }

    /**
     * Get station time at the end of last command processing.
     *
     * @return Time as unixtime
     */
    int getStationTime() {
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
    public byte getMode() {
        return mMode;
    }

    /**
     * Get configurable station number (an active point number to work at).
     *
     * @return Active point number, zero for chip initialization
     */
    public byte getNumber() {
        return mNumber;
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
    public int getLastInitTime() {
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
     * Get the time of last chip registration.
     *
     * @return Text representation of time of last chip registration in local timezone
     */
    public String getLastChipTimeString() {
        if (mLastChipTime == 0) return "-";
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mLastChipTime * 1000L);
        final DateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        return format.format(calendar.getTime());
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
        } catch (IOException closeException) {
            mSocket = null;
        }
    }

    /**
     * Write command to station input stream.
     *
     * @param command Command payload without start and stop bytes
     * @return True if the command was sent
     */
    private boolean send(final byte[] command) {
        if (command.length > (PACKET_SIZE - 4)) return false;
        byte[] buffer = new byte[PACKET_SIZE];
        buffer[0] = HEADER_SIGNATURE;
        buffer[1] = HEADER_SIGNATURE;
        buffer[2] = HEADER_SIGNATURE;
        buffer[3] = HEADER_SIGNATURE;
        System.arraycopy(command, 0, buffer, 4, command.length);
        try {
            final OutputStream output = mSocket.getOutputStream();
            output.write(buffer);
            output.flush();
        } catch (IOException e) {
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
        final byte[] buffer = new byte[PACKET_SIZE];
        try {
            final InputStream input = mSocket.getInputStream();
            while (System.currentTimeMillis() - mStartTime < WAIT_TIMEOUT) {
                // wait for data to appear in input stream
                if (input.available() == 0) {
                    sleep();
                    continue;
                }
                // read data into PACKET_SIZE byte buffer
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
                // stop waiting for more data if we got round number of PACKET_SIZE byte blocks
                // and last block does not have continuation flag
                if (response.length % PACKET_SIZE == 0
                        && response[response.length - PACKET_SIZE + 5] != (byte) 0x80) {
                    return response;
                }
            }
            return response;
        } catch (IOException e) {
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
        if (receiveBuffer.length == 0) return new byte[]{REC_TIMEOUT};
        // check if we got response as several PACKET_SIZE blocks
        if (receiveBuffer.length % PACKET_SIZE != 0) return new byte[]{REC_BAD_RESPONSE};
        // process all blocks
        final byte responseCode = (byte) (sendBuffer[0] + 0x10);
        byte[] response = new byte[]{COMMAND_OK};
        for (int n = 0; n < receiveBuffer.length / PACKET_SIZE; n++) {
            final int start = n * PACKET_SIZE;
            // check header
            if (receiveBuffer[start] != HEADER_SIGNATURE
                    || receiveBuffer[start + 1] != HEADER_SIGNATURE
                    || receiveBuffer[start + 2] != HEADER_SIGNATURE
                    || receiveBuffer[start + 3] != HEADER_SIGNATURE) {
                return new byte[]{REC_BAD_RESPONSE};
            }
            // check if command code received is equal to command code sent
            if (receiveBuffer[start + 4] != responseCode) return new byte[]{REC_BAD_RESPONSE};
            // check payload length
            if (receiveBuffer[start + 5] == 0) return new byte[]{REC_BAD_RESPONSE};
            // check command execution code (it should be present and equal to zero)
            if (receiveBuffer[start + 6] != 0) {
                return new byte[]{(byte) (REC_COMMAND_ERROR + receiveBuffer[start + 6])};
            }
            // check zero finish byte
            if (receiveBuffer[start + 31] != (byte) 0x00) return new byte[]{REC_BAD_RESPONSE};
            // copy payload to response buffer
            final int oldLen = response.length;
            final int newLen = receiveBuffer[start + 5] - 1;
            final byte[] temp = new byte[oldLen + newLen];
            System.arraycopy(response, 0, temp, 0, oldLen);
            System.arraycopy(receiveBuffer, start + 7, temp, oldLen, newLen);
            response = temp;
        }
        return response;
    }

    /**
     * Call runCommand (which performs communication with a station),
     * receive response, check it, set mLastError in case of error and fill
     * responseContent with actual station response.
     *
     * @param commandContent  Command payload sent to station
     * @param responseContent Station response without service bytes
     * @return True if there was no communication or command execution errors
     */
    private boolean command(final byte[] commandContent, final byte[] responseContent) {
        mLastError = 0;
        // Save time at the beginning of command processing
        mStartTime = System.currentTimeMillis();
        // Communicate with the station
        final byte[] rawResponse = runCommand(commandContent);
        // Compute execution time
        final long now = System.currentTimeMillis();
        mResponseTime = now - mStartTime;
        // Check for command execution errors and response parsing errors
        if (rawResponse[0] != COMMAND_OK) {
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
                    mLastError = R.string.err_station_wrong;
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
        // Everything is OK, get station clock current time and drift
        mStationTime = byteArray2Int(rawResponse, 1, 4);
        mTimeDrift = mStationTime - (int) (now / 1000L);
        // Everything is OK, copy station response content
        System.arraycopy(rawResponse, 1, responseContent, 0, responseContent.length);
        return true;
    }

    /**
     * Get the last communication error.
     *
     * @return Code of last error or zero (if no errors occurred)
     */
    public int getLastError() {
        return mLastError;
    }

    /**
     * Convert byte array section to int.
     *
     * @param array Array of bytes
     * @param start Starting position of byte sequence which will be converted to int
     * @param end   Ending position of byte sequence
     * @return Int representation of byte sequence
     */
    private int byteArray2Int(final byte[] array, final int start, final int end) {
        int result = 0;
        for (int i = start; i <= end; i++) {
            result = result | (array[i] & 0xFF) << ((end - i) * 8);
        }
        return result;
    }

    /**
     * Copy first count bytes from int value to the section of byte array.
     *
     * @param value    Int value to copy
     * @param array    Target byte array
     * @param starting Starting position in byte array to copy int value
     * @param count    Number of bytes to copy
     */
    private void int2ByteArray(final int value, final byte[] array, final int starting,
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
     * @param mode New station mode (chip initialization, ordinary or finish point)
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean newMode(final byte mode) {
        byte[] commandData = new byte[4];
        commandData[0] = (byte) 0x80;
        commandData[1] = (byte) 0;  // TODO: change station bios to remove extra byte
        commandData[2] = mode;
        commandData[3] = mNumber;
        // Send it to station
        final byte[] response = new byte[4];
        return command(commandData, response);
    }

    /**
     * Set station clock to Android local time converted to UTC timezone.
     *
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean syncTime() {
        byte[] commandData = new byte[8];
        commandData[0] = (byte) 0x81;
        commandData[1] = (byte) 0;  // TODO: change station bios to remove extra byte
        // Get current UTC time
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        commandData[2] = (byte) (calendar.get(Calendar.YEAR) - 2000);
        commandData[3] = (byte) (calendar.get(Calendar.MONTH) + 1);
        commandData[4] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        commandData[5] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        commandData[6] = (byte) calendar.get(Calendar.MINUTE);
        commandData[7] = (byte) (calendar.get(Calendar.SECOND));
        // Send it to station
        final byte[] response = new byte[4];
        return command(commandData, response);
    }

    /**
     * Reset station by giving it new number and erasing all data in it.
     *
     * @param number New station number
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean resetStation(final byte number) {
        byte[] commandData = new byte[12];
        commandData[0] = (byte) 0x82;
        commandData[1] = (byte) 0;  // TODO: change station bios to remove extra byte
        commandData[2] = number;
        commandData[3] = mNumber;
        int2ByteArray(mChipsRegistered, commandData, 4, 4);
        int2ByteArray(mLastChipTime, commandData, 8, 4);
        // Send it to station
        final byte[] response = new byte[4];
        if (!command(commandData, response)) return false;
        // Update station number in class object
        mNumber = number;
        return true;
    }

    /**
     * Get station local time, mode, number, etc.
     *
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean fetchStatus() {
        // Get response from station
        final byte[] response = new byte[12];
        if (!command(new byte[]{(byte) 0x83}, response)) return false;
        // Get station mode
        mMode = response[4];
        // Get station N
        mNumber = response[5];
        // Get number of chips registered by station
        mChipsRegistered = byteArray2Int(response, 6, 7);
        // Get last chips registration time
        mLastChipTime = byteArray2Int(response, 8, 11);
        return true;
    }

    /**
     * Init a chip with team number and members mask.
     *
     * @param teamNumber Team number
     * @param teamMask   Mask of team members presence
     * @return True if succeeded
     */
    public boolean initChip(final int teamNumber, final int teamMask) {
        // Note: last 4 byte are reserved and equal to zero now
        byte[] commandData = new byte[16];
        // Prepare command payload
        commandData[0] = (byte) 0x84;
        commandData[1] = (byte) 0;  // TODO: change station bios to remove extra byte
        int2ByteArray(teamNumber, commandData, 2, 2);
        final int now = (int) (System.currentTimeMillis() / 1000L);
        // TODO: station should use its own clock
        int2ByteArray(now, commandData, 4, 4);
        int2ByteArray(teamMask, commandData, 8, 4);
        // Send command to station
        final byte[] response = new byte[11];
        if (!command(commandData, response)) return false;
        // TODO: get init time from station response
        mLastInitTime = mStationTime;
        return true;
    }

}
