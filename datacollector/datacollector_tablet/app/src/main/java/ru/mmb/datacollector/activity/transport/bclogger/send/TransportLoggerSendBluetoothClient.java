package ru.mmb.datacollector.activity.transport.bclogger.send;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import ru.mmb.datacollector.activity.transport.bclogger.receive.TransportLoggerReceiveBluetoothServer;
import ru.mmb.datacollector.bluetooth.BluetoothClient;
import ru.mmb.datacollector.bluetooth.DeviceInfo;
import ru.mmb.datacollector.transport.exporter.DataExtractorToJson;
import ru.mmb.datacollector.transport.exporter.ExportMode;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;

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

    public void sendLoggerData() {
        writeToConsole("");
        String loggerData = generateLoggerDataJSON();
        if (loggerData == null) {
            writeToConsole("no data to export");
            return;
        }
        boolean connected = connect();
        if (connected) {
            boolean success = sendDataWithEndOfMessage(loggerData, COMM_SILENT);
            if (success) {
                // wait for transaction OK signal
                receiveDataWithEndOfMessage(COMM_VERBOSE);
            }
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }

    private String generateLoggerDataJSON() {
        DataExtractorToJson dataExtractor = new DataExtractorToJson(ExportMode.FULL);
        MetaTable table = MetaTablesRegistry.getInstance().getTableByName("RawLoggerData");
        table.setExportWhereAppendix("");
        JSONObject mainContainer = new JSONObject();
        JSONArray records = new JSONArray();
        dataExtractor.setTargetRecords(records);
        dataExtractor.setCurrentTable(table);
        try {
            if (dataExtractor.hasRecordsToExport()) {
                dataExtractor.exportNewRecords(new ExportState());
            }
            mainContainer.put("RawLoggerData", records);
        } catch (Exception e) {
            return null;
        }
        return mainContainer.toString();
    }
}
