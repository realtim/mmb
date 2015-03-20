package ru.mmb.datacollector.activity.input.bclogger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.mmb.datacollector.model.bclogger.LoggerInfo;

public abstract class BluetoothClient {
    public static final Pattern REGEXP_SCANNER_ID = Pattern.compile("-Scanner ID: (\\d{2})");
    public static final Pattern REGEXP_SCANPOINT_ORDER = Pattern.compile("-Control Point: (\\d{2})");
    public static final Pattern REGEXP_LENGTH_CHECKING = Pattern.compile("-Barcode string length checking: ([YN])");
    public static final Pattern REGEXP_NUMBERS_ONLY = Pattern.compile("-Numbers only: ([YN])");
    public static final Pattern REGEXP_PATTERN = Pattern.compile("-Barcode pattern: (\\S+)");
    public static final Pattern REGEXP_DATE_TIME = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2})");

    private static final SimpleDateFormat loggerDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat loggerTimeFormat = new SimpleDateFormat("HH:mm:ss");

    private static final UUID LOGGER_SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Context context;
    private final LoggerInfo loggerInfo;
    private final Handler handler;

    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private boolean terminated = false;

    public BluetoothClient(Context context, LoggerInfo loggerInfo, Handler handler) {
        this.context = context;
        this.loggerInfo = loggerInfo;
        this.handler = handler;
    }

    public OutputStream getOutStream() {
        return outStream;
    }

    public InputStream getInStream() {
        return inStream;
    }

    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void terminate() {
        this.terminated = true;
    }

    public void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    public void sendFinishedNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED));
        }
    }

    public boolean connect() {
        // create socket
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(loggerInfo.getLoggerBTAddress());
        try {
            // !!! On HTC standard variant is not working.
            // btSocket = device.createRfcommSocketToServiceRecord(LOGGER_SERVICE_UUID);
            Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            btSocket = (BluetoothSocket) m.invoke(device, 1);
        } catch (Exception e) {
            writeToConsole("socket create failed: " + e.getMessage());
            btSocket = null;
            return false;
        }
        // connect
        btAdapter.cancelDiscovery();
        try {
            btSocket.connect();
            writeToConsole("connected to " + loggerInfo.getLoggerName());
        } catch (IOException e) {
            Log.e("BTCLIENT", "socket connect error", e);
            writeToConsole("can't connect to " + loggerInfo.getLoggerName());
            safeCloseBTSocket();
            return false;
        }
        // open communication streams
        try {
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();
        } catch (IOException e) {
            writeToConsole("communication streams creation failed: " + e.getMessage());
            safeCloseBTSocket();
            return false;
        }
        return true;
    }

    private void safeCloseBTSocket() {
        if (outStream != null) {
            try {
                outStream.close();
            } catch (IOException e) {
            }
            outStream = null;
        }
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
            }
            inStream = null;
        }
        try {
            btSocket.close();
        } catch (IOException e) {
            writeToConsole("unable to close socket during connection failure: " + e.getMessage());
        }
        btSocket = null;
    }

    public String sendRequestWaitForReply(String message) {
        return sendRequestWaitForReply(message, 1000);
    }

    public String sendRequestWaitForReply(String message, long waitTimeout) {
        byte[] msgBuffer = message.getBytes();
        byte[] Buffer = new byte[8192];
        StringBuffer inBuff = new StringBuffer();

        writeToConsole("sending: " + message);
        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            writeToConsole("exception occurred during write: " + e.getMessage());
            return null;
        }
        writeToConsole("reading reply");
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Receiving data...");
            wl.acquire();
            long time_old = System.currentTimeMillis();
            while (!isTerminated() && System.currentTimeMillis() - time_old < waitTimeout) {
                while (!isTerminated() && inStream.available() > 0) {
                    int bytes = inStream.read(Buffer);
                    inBuff.append(new String(Buffer, 0, bytes));
                    writeToConsole("total bytes read: " + inBuff.length());
                    time_old = System.currentTimeMillis();
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            wl.release();
        } catch (IOException e) {
            writeToConsole("exception occurred during read: " + e.getMessage());
            return null;
        }
        writeToConsole("reply read: " + inBuff.toString());
        return inBuff.toString();
    }

    public void disconnectImmediately() {
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                writeToConsole("unable to flust outStream: " + e.getMessage());
            }
        }
        if (btSocket != null) {
            safeCloseBTSocket();
        }
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

    protected void updateLoggerTime() {
        sendRequestWaitForReply("SETT\n");
        Date currentTime = new Date();
        String timeString =
                loggerDateFormat.format(currentTime) + " " + loggerTimeFormat.format(currentTime);
        sendRequestWaitForReply(timeString + "\n", 5000);
    }
}
