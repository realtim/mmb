package ru.mmb.datacollector.activity.transport.bclogger.receive;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;

public class TransportLoggerReceiveBluetoothServer {
    public static final String BLUETOOTH_SERVICE_NAME = "DatacollectorReportDevice";
    public static final UUID BLUETOOTH_SERVICE_UUID = UUID.fromString("747034d7-0266-44fd-92fe-da39c10468b0");

    private final Handler handler;

    private BluetoothServerSocket serverSocket = null;

    private boolean terminated = false;

    public TransportLoggerReceiveBluetoothServer(Handler handler) {
        this.handler = handler;
    }

    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void terminate() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
        this.terminated = true;
    }

    public void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    public void sendAcceptErrorNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_ACCEPT_ERROR));
        }
    }

    public void sendAcceptSuccessNotification(BluetoothSocket socket) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_ACCEPT_SUCCESS, socket));
        }
    }

    public void accept() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // create a new listening server socket
        try {
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(BLUETOOTH_SERVICE_NAME,
                    BLUETOOTH_SERVICE_UUID);
        } catch (IOException e) {
            writeToConsole("error on server socket creation: " + e.getMessage());
            sendAcceptErrorNotification();
            return;
        }
        try {
            // this is a blocking call and will only return on a
            // successful connection or an exception or close
            writeToConsole("listening for new connection");
            BluetoothSocket socket = serverSocket.accept();
            writeToConsole("client connected");
            sendAcceptSuccessNotification(socket);
        } catch (IOException e) {
            writeToConsole("error accepting client: " + e.getMessage());
            sendAcceptErrorNotification();
        } finally {
            safeCloseServerSocket();
        }
    }

    private void safeCloseServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }
}
