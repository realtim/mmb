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

    protected void writeToConsole(String message) {
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
        byte[] buffer = message.getBytes();
        writeToConsole("sending: " + message);
        try {
            outStream.write(buffer);
        } catch (IOException e) {
            writeToConsole("exception occurred during write: " + e.getMessage());
            return false;
        }
        writeToConsole("data sent");
        return true;
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

    protected String sendRequestWaitForReply(String message) {
        return sendRequestWaitForReply(message, 1000, true);
    }

    protected String sendRequestWaitForReplySilent(String message) {
        return sendRequestWaitForReply(message, 1000, false);
    }

    protected String sendRequestWaitForReply(String message, long waitTimeout, boolean writeReplyToConsole) {
        boolean success = sendData(message);
        if (success) {
            return receiveData(waitTimeout, writeReplyToConsole);
        } else {
            return null;
        }
    }
}
