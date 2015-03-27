package ru.mmb.datacollector.activity.transport.bclogger.send;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

import ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveBluetoothServer;
import ru.mmb.datacollector.bluetooth.BluetoothClient;
import ru.mmb.datacollector.bluetooth.DeviceInfo;

public class TransportLoggerSendBluetoothClient extends BluetoothClient {
    private final DeviceInfo deviceInfo;

    public TransportLoggerSendBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler) {
        super(context, handler);
        this.deviceInfo = deviceInfo;
    }

    private boolean connect() {
        // create socket
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceInfo.getDeviceBTAddress());
        try {
            BluetoothSocket btSocket = device.createRfcommSocketToServiceRecord(TransportLoggerReceiveBluetoothServer.BLUETOOTH_SERVICE_UUID);
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

    public void sendLoggerData(String data) {
        boolean connected = connect();
        if (connected) {
            boolean success = sendData(data);
            if (success) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                receiveData(1000, true);
            }
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }
}
