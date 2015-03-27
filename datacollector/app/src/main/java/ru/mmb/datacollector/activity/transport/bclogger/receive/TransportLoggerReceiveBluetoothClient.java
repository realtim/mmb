package ru.mmb.datacollector.activity.transport.bclogger.receive;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;

import ru.mmb.datacollector.bluetooth.BluetoothClient;

public class TransportLoggerReceiveBluetoothClient extends BluetoothClient {
    private boolean streamsOpened = false;

    public TransportLoggerReceiveBluetoothClient(Context context, Handler handler, BluetoothSocket socket) {
        super(context, handler);
        setSocket(socket);
        this.streamsOpened = openCommunicationStreams();
    }

    public void receive() {
        String loggerData = null;
        try {
            if (streamsOpened) {
                loggerData = receiveData(1000, true);
                if (loggerData == null) {
                    sendFinishedErrorNotification();
                    return;
                }
                if (!sendData("DATA RECEIVED")) {
                    sendFinishedErrorNotification();
                    return;
                }
            }
        } finally {
            disconnectImmediately();
        }
        parseAndSaveLoggerData(loggerData);
        sendFinishedSuccessNotification();
    }

    private void parseAndSaveLoggerData(String loggerData) {
        // TODO implement
        writeToConsole("received data: " + loggerData);
    }
}
