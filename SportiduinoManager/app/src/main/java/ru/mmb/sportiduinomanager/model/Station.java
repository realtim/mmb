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
public class Station {
    /**
     * Station mode for chips initialization.
     */
    public static final byte STATION_MODE_INIT = 0;
    /**
     * Station mode for ordinary active point.
     */
    public static final byte STATION_MODE_ORDINARY = 1;
    /**
     * Station mode for active point at segment finish.
     */
    public static final byte STATION_MODE_FINISH = 2;

    /**
     * Timeout (im ms) while waiting for station response.
     */
    private static final int WAIT_TIMEOUT = 50000;

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
     * Time waiting for station while receiving station response.
     */
    private long mResponseTime;

    /**
     * Time difference in seconds between the station and Android.
     */
    private int mTimeDrift;
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
        calendar.setTimeInMillis(mLastChipTime * 1000);
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

    // Write command to station input stream
    private boolean send(final byte[] command) {
        byte[] buffer = new byte[command.length + 4];
        buffer[0] = (byte) 0xFE;
        buffer[1] = (byte) 0xFE;
        buffer[2] = (byte) 0xFE;
        buffer[3] = (byte) 0xFE;
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

    // Read raw data in 32-byte blocks from station output stream
    private byte[] receive() {
        byte[] response = null;
        final byte[] buffer = new byte[32];
        mResponseTime = -1;
        try {
            final long begin = System.currentTimeMillis();
            final InputStream input = mSocket.getInputStream();
            while (System.currentTimeMillis() - begin < WAIT_TIMEOUT) {
                // wait for data to appear in input stream
                if (input.available() == 0) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        return null;
                    }
                    continue;
                }
                // read data into 32-byte buffer
                final int newLen = input.read(buffer);
                if (newLen == 0) continue;
                // add received bytes (32 or less) to response array
                if (response == null) {
                    response = new byte[newLen];
                    System.arraycopy(buffer, 0, response, 0, newLen);
                } else {
                    final int oldLen = response.length;
                    final byte[] temp = new byte[oldLen + newLen];
                    System.arraycopy(response, 0, temp, 0, oldLen);
                    System.arraycopy(buffer, 0, temp, oldLen, newLen);
                    response = temp;
                }
                // stop waiting for more data if we got round number of 32-byte blocks
                // and last block does not have continuation flag
                if (response.length % 32 == 0
                        && response[response.length - 32 + 5] != (byte) 0x80) {
                    mResponseTime = System.currentTimeMillis() - begin;
                    return response;
                }
            }
            mResponseTime = System.currentTimeMillis() - begin;
            return response;
        } catch (IOException e) {
            // station got disconnected
            disconnect();
            return null;
        }
    }

    // Send command to station, receive response and make response integrity checks
    private byte[] runCommand(final byte[] sendBuffer) {
        // reconnect (just in case and send the command
        if (!connect()) return new byte[]{SEND_FAILED};
        if (!send(sendBuffer)) return new byte[]{SEND_FAILED};
        // get station response
        final byte[] receiveBuffer = receive();
        if (receiveBuffer == null) return new byte[]{REC_TIMEOUT};
        // check if we got response as several 32-byte blocks
        if (receiveBuffer.length % 32 != 0) return new byte[]{REC_BAD_RESPONSE};
        // process all blocks
        final byte responseCode = (byte) (sendBuffer[0] + 0x10);
        byte[] response = new byte[]{COMMAND_OK};
        for (int n = 0; n < receiveBuffer.length / 32; n++) {
            // check header
            if (receiveBuffer[n * 32] != (byte) 0xfe
                    || receiveBuffer[n * 32 + 1] != (byte) 0xfe
                    || receiveBuffer[n * 32 + 2] != (byte) 0xfe
                    || receiveBuffer[n * 32 + 3] != (byte) 0xfe) {
                return new byte[]{REC_BAD_RESPONSE};
            }
            // check if command code received is equal to command code sent
            if (receiveBuffer[n * 32 + 4] != responseCode) return new byte[]{REC_BAD_RESPONSE};
            // check payload length
            if (receiveBuffer[n * 32 + 5] == 0) return new byte[]{REC_COMMAND_ERROR};
            // check command execution code (it should be present and equal to zero)
            if (receiveBuffer[n * 32 + 6] != 0) return new byte[]{REC_COMMAND_ERROR};
            // check zero finish byte
            if (receiveBuffer[n * 32 + 31] != (byte) 0x00) return new byte[]{REC_BAD_RESPONSE};
            // copy payload to response buffer
            final int oldLen = response.length;
            final int newLen = receiveBuffer[n * 32 + 5] - 1;
            final byte[] temp = new byte[oldLen + newLen];
            System.arraycopy(response, 0, temp, 0, oldLen);
            System.arraycopy(receiveBuffer, n * 32 + 7, temp, oldLen, newLen);
            response = temp;
        }
        return response;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean command(final byte[] commandContent, final byte[] responseContent) {
        mLastError = 0;
        // Communicate with the station
        final byte[] rawResponse = runCommand(commandContent);
        // Sanity check
        if (rawResponse == null || rawResponse.length == 0) {
            mLastError = R.string.err_bt_internal_error;
            return false;
        }
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
                case REC_COMMAND_ERROR:
                    mLastError = R.string.err_bt_receive_command_failed;
                    return false;
                default:
                    mLastError = R.string.err_bt_internal_error;
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
     * Get the last communication error.
     *
     * @return Code of last error or zero (if no errors occurred)
     */
    public int getLastError() {
        return mLastError;
    }

    // Convert byte array section to int
    private int byteArray2Int(final byte[] array, final int start, final int end) {
        int result = 0;
        for (int i = start; i <= end; i++) {
            result = result | (array[i] & 0xff) << ((end - i) * 8);
        }
        return result;
    }

    // Copy 4-byte int value to the section of byte array
    private void int2byte_array(final int value, final byte[] array, final int from) {
        byte[] converted = new byte[4];
        for (int i = 0; i <= 3; i++) {
            converted[0] = (byte) ((value >> 8 * (3 - i)) & 0xff);
        }
        System.arraycopy(converted, 0, array, from, 4);
    }

    /**
     * Get station local time, mode, number, etc.
     *
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean getStatus() {
        // Get response from station
        final byte[] response = new byte[12];
        if (!command(new byte[]{(byte) 0x83}, response)) return false;
        // Get station clock drift
        mTimeDrift = byteArray2Int(response, 0, 3) - (int) (System.currentTimeMillis() / 1000L);
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
     * Set station mode and number.
     *
     * @param mode New station mode (chip initialization, ordinary or finish point)
     * @return True if we got valid response from station, check mLastError otherwise
     */
    public boolean setMode(final byte mode) {
        byte[] commandData = new byte[4];
        commandData[0] = (byte) 0x80;
        commandData[1] = (byte) 0;  // TODO: change station bios to remove extra byte
        commandData[2] = mode;
        commandData[3] = mNumber;
        // Send it to station
        final byte[] response = new byte[4];
        if (!command(commandData, response)) return false;
        // Get current station clock drift
        mTimeDrift = byteArray2Int(response, 0, 3) - (int) (System.currentTimeMillis() / 1000L);
        return true;
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
        // Set 1sec in future due to delays in station communication
        commandData[7] = (byte) (calendar.get(Calendar.SECOND) + 1);
        // Send it to station
        final byte[] response = new byte[4];
        if (!command(commandData, response)) return false;
        // Get new station clock drift after time change
        mTimeDrift = byteArray2Int(response, 0, 3) - (int) (System.currentTimeMillis() / 1000L);
        return true;
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
        int2byte_array(mChipsRegistered, commandData, 4);
        int2byte_array(mLastChipTime, commandData, 8);
        // Send it to station
        final byte[] response = new byte[4];
        if (!command(commandData, response)) return false;
        // Update station number in class object
        mNumber = number;
        // Get current station clock drift
        mTimeDrift = byteArray2Int(response, 0, 3) - (int) (System.currentTimeMillis() / 1000L);
        return true;
    }
}