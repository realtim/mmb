package ru.mmb.loggermanager.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LoggerBluetoothClient extends BluetoothClient {
    public static final Pattern REGEXP_SCANNER_ID = Pattern.compile("-Scanner ID: (\\d{2})");
    public static final Pattern REGEXP_SCANPOINT_ORDER = Pattern.compile("-Control Point: (\\d{2})");
    public static final Pattern REGEXP_LENGTH_CHECKING = Pattern.compile("-Barcode string length checking: ([YN])");
    public static final Pattern REGEXP_NUMBERS_ONLY = Pattern.compile("-Numbers only: ([YN])");
    public static final Pattern REGEXP_PATTERN = Pattern.compile("-Barcode pattern: (\\S+)");
    public static final Pattern REGEXP_DATE_TIME = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2})");

    private static final SimpleDateFormat loggerDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat loggerTimeFormat = new SimpleDateFormat("HH:mm:ss");

    private static final UUID LOGGER_SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final DeviceInfo deviceInfo;

    public LoggerBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler) {
        super(context, handler);
        this.deviceInfo = deviceInfo;
    }

    public boolean connect() {
        // create socket
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceInfo.getDeviceBTAddress());
        try {
            // !!! On HTC standard variant is not working.
            // btSocket = device.createRfcommSocketToServiceRecord(LOGGER_SERVICE_UUID);
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            BluetoothSocket btSocket = (BluetoothSocket) m.invoke(device, 1);
            setSocket(btSocket);
        } catch (Exception e) {
            writeToConsole("socket create failed: " + e.getMessage());
            setSocket(null);
            return false;
        }
        // connect
        btAdapter.cancelDiscovery();
        try {
            getSocket().connect();
            writeToConsole("connected to " + deviceInfo.getDeviceName());
        } catch (IOException e) {
            Log.e("BTCLIENT", "socket connect error", e);
            writeToConsole("can't connect to " + deviceInfo.getDeviceName());
            safeCloseSocket();
            return false;
        }
        // open communication streams
        return openCommunicationStreams();
    }

    public String getValueFromReplyByRegexp(String[] replyStrings, Pattern pattern) {
        for (String replyString : replyStrings) {
            Matcher matcher = pattern.matcher(replyString);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public String getWholeStringFromReplyByRegexp(String[] replyStrings, Pattern pattern) {
        for (String replyString : replyStrings) {
            Matcher matcher = pattern.matcher(replyString);
            if (matcher.find()) {
                return replyString;
            }
        }
        return null;
    }

    protected boolean updateLoggerTime() {
        sendRequestWaitForReply("SETT\n", 300, true);
        Date currentTime = new Date();
        String timeString =
                loggerDateFormat.format(currentTime) + " " + loggerTimeFormat.format(currentTime);
        sendRequestWaitForReply(timeString + "\n", 100, true);
        return true;
    }
}
