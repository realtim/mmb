package ru.mmb.sportiduinomanager.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import ru.mmb.sportiduinomanager.R;

/**
 * Provides low level access to a Bluetooth station.
 */
public class StationRaw {
    /**
     * Code of setMode station command.
     */
    static final byte CMD_SET_MODE = (byte) 0x80;
    /**
     * Code of setTime station command.
     */
    static final byte CMD_SET_TIME = (byte) 0x81;
    /**
     * Code of resetStation station command.
     */
    static final byte CMD_RESET_STATION = (byte) 0x82;
    /**
     * Code of getStatus station command.
     */
    static final byte CMD_GET_STATUS = (byte) 0x83;
    /**
     * Code of chipInit station command.
     */
    static final byte CMD_INIT_CHIP = (byte) 0x84;
    /**
     * Code of getLastTeams station command.
     */
    static final byte CMD_LAST_TEAMS = (byte) 0x85;
    /**
     * Code of getTeamRecord station command.
     */
    static final byte CMD_TEAM_RECORD = (byte) 0x86;
    /**
     * Code of readCardPage station command.
     */
    static final byte CMD_READ_CARD = (byte) 0x87;
    /**
     * Code of updateTeamMask station command.
     */
    static final byte CMD_UPDATE_MASK = (byte) 0x88;
    /**
     * Code of readFlash station command.
     */
    static final byte CMD_READ_FLASH = (byte) 0x8a;
    /**
     * Code of getConfig station command.
     */
    static final byte CMD_GET_CONFIG = (byte) 0x8d;
    /**
     * Default UUID of station Bluetooth socket.
     */
    private static final UUID STATION_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /**
     * Default maximum size of communication packet.
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
     * Timeout (im ms) while waiting for station response.
     */
    private static final int WAIT_TIMEOUT = 30_000;
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
     * Result of sending command to station: wrong signature.
     */
    private static final byte REC_BAD_SIGNATURE = 3;
    /**
     * Result of sending command to station: no payload.
     */
    private static final byte REC_BAD_NOPAYLOAD = 4;
    /**
     * Result of sending command to station: wrong payload length.
     */
    private static final byte REC_BAD_PAYLOADLEN = 5;
    /**
     * Result of sending command to station: wrong station number.
     */
    private static final byte REC_BAD_STATIONNUM = 6;
    /**
     * Result of sending command to station: wrong CRC.
     */
    private static final byte REC_BAD_CRC = 7;
    /**
     * Result of sending command to station: wrong command code.
     */
    private static final byte REC_BAD_COMMANDCODE = 8;
    /**
     * Result of sending command to station: station sent an error.
     * of command execution
     */
    private static final byte REC_COMMAND_ERROR = 9;

    /**
     * Station Bluetooth whole object.
     */
    private final BluetoothDevice mDevice;
    /**
     * Connected station Bluetooth socket.
     */
    private BluetoothSocket mSocket;
    /**
     * Maximum size of communication packet for connected station.
     */
    private int mMaxPacketSize;
    /**
     * Configurable station number
     * (an control point number to work at or zero for chip initialization).
     */
    private int mNumber;
    /**
     * Android time at the start of command processing.
     */
    private long mStartTime;
    /**
     * Time waiting for station while receiving station response.
     */
    private long mResponseTime;
    /**
     * Code of last error of communication with station.
     */
    private int mLastError;
    /**
     * True when StationQuerying is allowed by activity to send commands to station.
     */
    private boolean mQueryingAllowed;
    /**
     * True when StationQuerying is in the middle of sending commands to station.
     */
    private boolean mQueryingActive;


    /**
     * Create Station from Bluetooth scan.
     *
     * @param device Bluetooth device handler
     */
    StationRaw(final BluetoothDevice device) {
        mDevice = device;
        mMaxPacketSize = MAX_PACKET_SIZE;
        mNumber = 0;
        mLastError = 0;
        mQueryingAllowed = false;
        mQueryingActive = false;
        // Create client socket with default Bluetooth UUID
        try {
            mSocket = mDevice.createRfcommSocketToServiceRecord(STATION_UUID);
        } catch (IOException | SecurityException ignored) {
            // Just let mSocket to stay equal to null
        }
    }


    /**
     * Set station max packet size.
     *
     * @param size max packet size (obtained from fetchConfig call)
     */
    void setMaxPacketSize(final int size) {
        mMaxPacketSize = size;
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
     * Set configurable station number (a control point number to work at).
     *
     * @param number New number for the station
     */
    void setNumber(final int number) {
        mNumber = number;
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
     * Get Android time at the start of command processing.
     *
     * @return Android unixtime
     */
    long getStartTime() {
        return mStartTime;
    }

    /**
     * Get the last communication error and reset it to zero.
     *
     * @param resetError Reset error to "no error" after returning its value
     * @return Code of last error or zero (if no errors occurred)
     */
    public int getLastError(final boolean resetError) {
        synchronized (this) {
            final int error = mLastError;
            if (resetError) {
                mLastError = 0;
            }
            return error;
        }
    }

    /**
     * Set the last communication error.
     *
     * @param error New error value (resource string id)
     */
    void setLastError(final int error) {
        synchronized (this) {
            mLastError = error;
        }
    }

    /**
     * Get station name.
     *
     * @return String with Bluetooth name
     */
    public String getName() {
        try {
            return mDevice.getName();
        } catch (SecurityException unused) {
            return "";
        }
    }

    /**
     * Get station MAC as string.
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
        synchronized (this) {
            mQueryingAllowed = isAllowed;
        }
    }

    /**
     * Flag the beginning/end of sending commands to station by StationQuerying.
     *
     * @param isActive True if querying is going to start
     */
    public void setQueryingActive(final boolean isActive) {
        synchronized (this) {
            mQueryingActive = isActive;
        }
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
        } catch (IOException | SecurityException connectException) {
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
     * Compute CRC8 checksum.
     *
     * @param array Array of bytes
     * @param end   ending position for computing crc
     * @return CRC8
     */
    private byte crc8(final byte[] array, final int end) {
        byte crc = 0x00;
        for (int i = 1; i < end; i++) {
            byte extract = array[i];
            for (byte tempI = 8; tempI != 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) (sum & 0xFF & 0x01);
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
     * Write command to station input stream.
     *
     * @param command Command payload without start and stop bytes
     * @return True if the command was sent
     */
    private boolean send(final byte[] command) {
        final int len = command.length - 1;
        if (len < 0 || (len + HEADER_SIZE + 1) > mMaxPacketSize) return false;
        // prepare output buffer
        final byte[] buffer = new byte[len + HEADER_SIZE + 1];
        buffer[0] = HEADER_SIGNATURE;                   // Signature
        buffer[1] = 0;                                  // Packet Id
        buffer[2] = (byte) (mNumber & 0xFF);            // Station number
        buffer[3] = command[0];                         // Command code
        buffer[4] = (byte) ((len & 0xFF00) >> 8);       // Payload length
        buffer[5] = (byte) (len & 0x00FF);              // Payload length
        System.arraycopy(command, 1, buffer, HEADER_SIZE, len);
        buffer[len + HEADER_SIZE] = crc8(buffer, len + HEADER_SIZE);
        // send output buffer to station Bluetooth socket
        try {
            @SuppressWarnings("PMD.CloseResource") final OutputStream output = mSocket.getOutputStream();
            output.write(buffer);
            output.flush();
        } catch (IOException e) {
            // station got disconnected
            disconnect();
            return false;
        }
        return true;
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
        final byte[] buffer = new byte[mMaxPacketSize];
        try {
            @SuppressWarnings("PMD.CloseResource") final InputStream input = mSocket.getInputStream();
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
                if (response.length <= HEADER_SIZE) continue;
                final int payloadLen = ((response[4] & 0xFF) << 8) + (response[5] & 0xFF);
                if (response.length >= (payloadLen + HEADER_SIZE + 1)) {
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
     * Print send/receive buffer to logcat.
     *
     * @param tag    Buffer name for logcat
     * @param buffer Data send to or received from the station
     */
    private void logBuffer(final String tag, final byte[] buffer) {
        StringBuilder message = new StringBuilder();
        for (byte b : buffer) {
            message.append(String.format("%02x ", b));
        }
        Log.i(tag, message.toString());
    }

    /**
     * Send command to station, receive response and make response integrity checks.
     *
     * @param sendBuffer Actual command payload without starting and ending bytes
     * @return Byte array with station response, first byte contains error code
     */
    private byte[] runCommand(final byte[] sendBuffer) {
        // print data sent to the station
        logBuffer("Sent to station", sendBuffer);
        // reconnect (just in case and send the command
        if (!connect()) return new byte[]{SEND_FAILED};
        if (!send(sendBuffer)) return new byte[]{SEND_FAILED};
        // get station response
        final byte[] receiveBuffer = receive();
        final int len = receiveBuffer.length;
        if (len == 0) return new byte[]{REC_TIMEOUT};
        // print data received from the station
        logBuffer("Received from station", receiveBuffer);
        // check signature
        if (receiveBuffer[0] != HEADER_SIGNATURE) return new byte[]{REC_BAD_SIGNATURE};
        // check if response has at minimum 1 byte payload
        if (len < HEADER_SIZE) return new byte[]{REC_BAD_NOPAYLOAD};
        // check buffer length
        final int payloadLen = ((receiveBuffer[4] & 0xFF) << 8) + (receiveBuffer[5] & 0xFF);
        if (payloadLen != len - HEADER_SIZE - 1) return new byte[]{REC_BAD_PAYLOADLEN};
        // check station number
        if (sendBuffer[0] != CMD_GET_STATUS && sendBuffer[0] != CMD_GET_CONFIG
                && sendBuffer[0] != CMD_RESET_STATION
                && (receiveBuffer[2] & 0xFF) != mNumber) return new byte[]{REC_BAD_STATIONNUM};
        // update station number for getStatus command
        if (sendBuffer[0] == CMD_GET_STATUS) mNumber = receiveBuffer[2] & 0xFF;
        // check crc
        if (receiveBuffer[len - 1] != crc8(receiveBuffer, len - 1)) return new byte[]{REC_BAD_CRC};
        // check if command code received is equal to command code sent
        if (receiveBuffer[3] != sendBuffer[0] + 0x10) return new byte[]{REC_BAD_COMMANDCODE};
        // check command execution code (it should be present and equal to zero)
        if (receiveBuffer[HEADER_SIZE] != 0) return new byte[]{(byte) (REC_COMMAND_ERROR + receiveBuffer[HEADER_SIZE])};
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
     * @return True if there was no communication or command execution errors
     */
    boolean command(final byte[] commandContent, final byte[] responseContent) {
        synchronized (this) {
            // Save time at the beginning of command processing
            mStartTime = System.currentTimeMillis();
            // Communicate with the station
            final byte[] rawResponse = runCommand(commandContent);
            // Compute execution time
            final long now = System.currentTimeMillis();
            mResponseTime = now - mStartTime;
            // Check for command execution errors and response parsing errors
            if (rawResponse[0] != ALL_OK) {
                switch (rawResponse[0]) {
                    case SEND_FAILED:
                        mLastError = R.string.err_bt_send_failed;
                        break;
                    case REC_TIMEOUT:
                        mLastError = R.string.err_bt_receive_timeout;
                        break;
                    case REC_BAD_SIGNATURE:
                        mLastError = R.string.err_bt_receive_bad_signature;
                        break;
                    case REC_BAD_NOPAYLOAD:
                        mLastError = R.string.err_bt_receive_bad_nopayload;
                        break;
                    case REC_BAD_PAYLOADLEN:
                        mLastError = R.string.err_bt_receive_bad_payloadlen;
                        break;
                    case REC_BAD_STATIONNUM:
                        mLastError = R.string.err_bt_receive_bad_stationnum;
                        break;
                    case REC_BAD_CRC:
                        mLastError = R.string.err_bt_receive_bad_crc;
                        break;
                    case REC_BAD_COMMANDCODE:
                        mLastError = R.string.err_bt_receive_bad_commandcode;
                        break;
                    case REC_COMMAND_ERROR + 1:
                        mLastError = R.string.err_station_wrong_number;
                        break;
                    case REC_COMMAND_ERROR + 2:
                        mLastError = R.string.err_station_read;
                        break;
                    case REC_COMMAND_ERROR + 3:
                        mLastError = R.string.err_station_write;
                        break;
                    case REC_COMMAND_ERROR + 4:
                        mLastError = R.string.err_station_init_chip;
                        break;
                    case REC_COMMAND_ERROR + 5:
                        mLastError = R.string.err_station_bad_chip;
                        break;
                    case REC_COMMAND_ERROR + 6:
                        mLastError = R.string.err_station_no_chip;
                        break;
                    case REC_COMMAND_ERROR + 7:
                        mLastError = R.string.err_station_buffer_overflow;
                        break;
                    case REC_COMMAND_ERROR + 8:
                        mLastError = R.string.err_station_reset_impossible;
                        break;
                    case REC_COMMAND_ERROR + 9:
                        mLastError = R.string.err_station_incorrect_uid;
                        break;
                    case REC_COMMAND_ERROR + 10:
                        mLastError = R.string.err_station_wrong_team;
                        break;
                    case REC_COMMAND_ERROR + 11:
                        mLastError = R.string.err_station_no_data;
                        break;
                    case REC_COMMAND_ERROR + 12:
                        mLastError = R.string.err_station_bad_command;
                        break;
                    case REC_COMMAND_ERROR + 13:
                        mLastError = R.string.err_station_erase_flash;
                        break;
                    case REC_COMMAND_ERROR + 14:
                        mLastError = R.string.err_station_bad_chip_type;
                        break;
                    default:
                        mLastError = R.string.err_station_unknown;
                        break;
                }
                return false;
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
    }
}
