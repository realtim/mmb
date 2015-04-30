package ru.mmb.datacollector.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BluetoothClient {
    public static final String END_OF_MESSAGE = "<!--EOF-->";
    public static final boolean COMM_SILENT = false;
    public static final boolean COMM_VERBOSE = true;

    private final Context context;
    private final Handler handler;

    private BluetoothSocket socket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private boolean terminated = false;

    public BluetoothClient(Context context, Handler handler) {
        this.context = context;
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

    public BluetoothSocket getSocket() {
        return socket;
    }

    public void setSocket(BluetoothSocket socket) {
        this.socket = socket;
    }

    public synchronized void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    protected void sendFinishedSuccessNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_SUCCESS));
        }
    }

    protected void sendFinishedErrorNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_ERROR));
        }
    }

    protected boolean openCommunicationStreams() {
        try {
            outStream = socket.getOutputStream();
            inStream = socket.getInputStream();
        } catch (IOException e) {
            writeToConsole("communication streams creation failed: " + e.getMessage());
            safeCloseSocket();
            return false;
        }
        return true;
    }

    protected void safeCloseSocket() {
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
            socket.close();
        } catch (IOException e) {
            writeToConsole("unable to close socket during connection failure: " + e.getMessage());
        }
        socket = null;
    }

    protected void disconnectImmediately() {
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                writeToConsole("unable to flust outStream: " + e.getMessage());
            }
        }
        if (socket != null) {
            safeCloseSocket();
        }
    }

    protected boolean sendData(String message) {
        return sendData(message, COMM_VERBOSE);
    }

    protected boolean sendData(String message, boolean writeMessageToConsole) {
        byte[] buffer = message.getBytes();
        if (writeMessageToConsole) {
            writeToConsole("sending: " + message);
        } else {
            writeToConsole("sending data");
        }
        try {
            int packageSize = 10000;
            for (int currentPos = 0; currentPos < buffer.length; currentPos += packageSize) {
                int bytesToSend = ((currentPos + packageSize) < buffer.length) ? packageSize :
                        buffer.length - currentPos;
                outStream.write(buffer, currentPos, bytesToSend);
                outStream.flush();
            }
        } catch (IOException e) {
            writeToConsole("exception occurred during write: " + e.getMessage());
            return false;
        }
        writeToConsole("data sent");
        return true;
    }

    protected boolean sendDataWithEndOfMessage(String message) {
        return sendDataWithEndOfMessage(message, COMM_VERBOSE);
    }

    protected boolean sendDataWithEndOfMessage(String message, boolean writeMessageToConsole) {
        return sendData(message + END_OF_MESSAGE, writeMessageToConsole);
    }

    protected String receiveData(long waitTimeout, boolean writeReplyToConsole) {
        byte[] readBuffer = new byte[8192];
        StringBuilder result = new StringBuilder(5 * 1024 * 1024);

        writeToConsole("reading reply");
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Receiving data...");
            wl.acquire();
            long timeOld = System.currentTimeMillis();
            int bytesRead = 0;
            while (!isTerminated() && System.currentTimeMillis() - timeOld < waitTimeout) {
                while (!isTerminated() && inStream.available() > 0) {
                    int bytes = inStream.read(readBuffer);
                    bytesRead += bytes;
                    result.append(new String(readBuffer, 0, bytes));
                    if (bytesRead > 30 * 1024) {
                        writeToConsole("total bytes read: " + result.length());
                        bytesRead = 0;
                    }
                    timeOld = System.currentTimeMillis();
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

        writeToConsole("total bytes read: " + result.length());
        if (writeReplyToConsole) {
            writeToConsole("reply read: " + result.toString());
        }

        return result.toString();
    }

    protected String receiveDataWithEndOfMessage(boolean writeReplyToConsole) {
        byte[] readBuffer;
        StringBuilder result = new StringBuilder(5 * 1024 * 1024);

        writeToConsole("reading reply");
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Receiving data...");
            wl.acquire();
            int bytesRead = 0;
            while (!isTerminated()) {
                int bytesInSession = 0;
                while (!isTerminated() && inStream.available() > 0) {
                    int bytesAvailable = inStream.available();
                    readBuffer = new byte[bytesAvailable];
                    int bytes = inStream.read(readBuffer, 0, bytesAvailable);
                    if (bytes > 0) {
                        bytesRead += bytes;
                        bytesInSession += bytes;
                        result.append(new String(readBuffer, 0, bytes));
                    }
                    if (bytesRead > 30 * 1024) {
                        writeToConsole("total bytes read: " + result.length());
                        bytesRead = 0;
                    }
                }
                // check end_of_message only if needed
                if (bytesInSession > 0 && result.length() > END_OF_MESSAGE.length()) {
                    if (checkEndOfMessageArrived(result)) {
                        writeToConsole("end of message marker received");
                        break;
                    }
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

        writeToConsole("total bytes read: " + result.length());
        if (writeReplyToConsole) {
            writeToConsole("reply read: " + result.toString());
        }

        // cut off end of message marker before return
        return result.toString().substring(0, result.length() - END_OF_MESSAGE.length());
    }

    private boolean checkEndOfMessageArrived(StringBuilder receivedBytes) {
        int markerLength = END_OF_MESSAGE.length();
        int receivedLength = receivedBytes.length();
        char[] lastChars = new char[markerLength];
        receivedBytes.getChars(receivedLength - markerLength, receivedLength, lastChars, 0);
        String lastCharsString = new String(lastChars);
        return END_OF_MESSAGE.equals(lastCharsString);
    }

    protected String sendRequestWaitForReply(String message) {
        return sendRequestWaitForReply(message, 1000, true);
    }

    protected String sendRequestWaitForReplySilent(String message) {
        return sendRequestWaitForReply(message, 1000, false);
    }

    protected String sendRequestWaitForReply(String message, long waitTimeout, boolean writeToConsole) {
        boolean success = sendData(message, writeToConsole);
        if (success) {
            return receiveData(waitTimeout, writeToConsole);
        } else {
            return null;
        }
    }
}
