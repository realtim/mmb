package ru.mmb.datacollector.activity.transport.bclogger.receive;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.bluetooth.BluetoothClient;
import ru.mmb.datacollector.transport.importer.DataSaver;
import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.model.MetaTable;
import ru.mmb.datacollector.transport.registry.MetaTablesRegistry;

public class TransportLoggerReceiveBluetoothClient extends BluetoothClient {
    private boolean streamsOpened = false;
    private ImportState importState = null;
    private ProgressThread progressThread = null;

    public TransportLoggerReceiveBluetoothClient(Context context, Handler handler, BluetoothSocket socket) {
        super(context, handler);
        setSocket(socket);
        this.streamsOpened = openCommunicationStreams();
    }

    public void receive() {
        writeToConsole("");
        String loggerData = null;
        try {
            if (streamsOpened) {
                loggerData = receiveDataWithEndOfMessage(COMM_SILENT);
                if (loggerData == null) {
                    sendFinishedErrorNotification();
                    return;
                }
                if (!sendDataWithEndOfMessage("OK")) {
                    sendFinishedErrorNotification();
                    return;
                }
                // don't break communication channel too early
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        } finally {
            disconnectImmediately();
        }
        parseAndSaveLoggerData(loggerData);
        sendFinishedSuccessNotification();
    }

    private void parseAndSaveLoggerData(String loggerData) {
        writeToConsole("data parsing started");
        importState = new ImportState();
        startProgressThread();
        try {
            JSONObject tables = new JSONObject(loggerData);
            DataSaver dataSaver = new DataSaver();
            MetaTable metaTable = MetaTablesRegistry.getInstance().getTableByName("RawLoggerData");
            dataSaver.setCurrentTable(metaTable);
            JSONArray tableRows = (JSONArray) tables.getJSONArray("RawLoggerData");
            resetImportState(metaTable, tableRows.length());
            importTableRows(dataSaver, tableRows);
            writeToConsole("SUCCESS imported rows count: " + tableRows.length());
        } catch (Exception e) {
            writeToConsole("ERROR data import: " + e.getMessage());
        }
        stopProgressThread();
    }

    private void startProgressThread() {
        progressThread = new ProgressThread(this);
        progressThread.start();
    }

    private void resetImportState(MetaTable metaTable, int totalRows) {
        importState.setCurrentTable(metaTable.getTableName());
        importState.setTotalRows(totalRows);
        importState.setRowsProcessed(0);
    }

    private void importTableRows(DataSaver dataSaver, JSONArray tableRows) throws JSONException {
        dataSaver.beginTransaction();
        Log.d("BT receive import", "started first transaction");
        for (int j = 0; j < tableRows.length(); j++) {
            // commit transaction one time in 200 rows
            if (((j + 1) % 200) == 0) {
                dataSaver.setTransactionSuccessful();
                dataSaver.endTransaction();
                Log.d("BT receive import", "records batch commited");
                // begin next transaction
                dataSaver.beginTransaction();
                Log.d("BT receive import", "started transaction");
            }
            try {
                dataSaver.saveRecordToDB(tableRows.getJSONObject(j));
            } catch (Exception e) {
                writeToConsole("Row not imported. " + tableRows.getJSONObject(j));
                writeToConsole("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
            incRowsProcessed();
        }
        dataSaver.setTransactionSuccessful();
        dataSaver.endTransaction();
        Log.d("BT receive import", "remaining records batch commited");
    }

    private void stopProgressThread() {
        progressThread.interrupt();
    }

    private synchronized int getRowsProcessed() {
        return importState.getRowsProcessed();
    }

    private synchronized void incRowsProcessed() {
        importState.incRowsProcessed();
    }

    private class ProgressThread extends Thread {
        private final TransportLoggerReceiveBluetoothClient owner;

        public ProgressThread(TransportLoggerReceiveBluetoothClient owner) {
            super("progress thread");
            this.owner = owner;
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    writeToConsole("rows processed: " + owner.getRowsProcessed());
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                // good exit from sleep
                Log.d("progress thread", "interrupted");
            }
        }
    }
}
