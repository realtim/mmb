package ru.mmb.datacollector.transport.importer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;
import ru.mmb.datacollector.util.JSONUtils;

public class Importer {
    public static final int ROWS_IN_BATCH = 200;

    private final ImportState importState;

    public Importer(ImportState importState) {
        this.importState = importState;
    }

    public void importPackageFromFile(String fileName) throws Exception {
        importState.appendMessage("Import started.");
        JSONObject tables = readJsonTablesPackage(fileName);
        importState.appendMessage("File loaded to JSON object.");
        importPackageFromJsonObject(tables);
    }

    public void importPackageFromJsonObject(JSONObject tables) throws Exception {
        DataSaver dataSaver = new DataSaver();
        JSONArray names = tables.names();
        for (int i = 0; i < names.length(); i++) {
            if (importState.isTerminated()) break;
            String tableName = names.getString(i);
            importState.appendMessage("Importing table: " + tableName);
            MetaTable metaTable = getMetaTable(tableName);
            if (metaTable == null) {
                importState.appendMessage("Meta table not found.");
                continue;
            }
            dataSaver.setCurrentTable(metaTable);
            if (metaTable.needClearBeforeImport()) {
                dataSaver.clearCurrentTable();
                Log.d("data saver", "table cleared: " + tableName);
            }
            JSONArray tableRows = tables.getJSONArray(tableName);
            resetImportState(metaTable, tableRows.length());
            importTableRows(dataSaver, tableRows);
        }
    }

    private JSONObject readJsonTablesPackage(String fileName) throws IOException, JSONException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(fileName), "UTF8");
        String jsonString = JSONUtils.readFromInputStream(reader, 32768);
        return new JSONObject(jsonString);
    }

    private MetaTable getMetaTable(String tableName) {
        return MetaTablesRegistry.getInstance().getTableByName(tableName);
    }

    private void resetImportState(MetaTable metaTable, int totalRows) {
        importState.setCurrentTable(metaTable.getTableName());
        importState.setTotalRows(totalRows);
        importState.setRowsProcessed(0);
    }

    private void importTableRows(DataSaver dataSaver, JSONArray tableRows) throws JSONException {
        dataSaver.beginTransaction();
        Log.d("DATA_SAVER", "started first transaction");
        for (int j = 0; j < tableRows.length(); j++) {
            if (needSaveBatch(j)) {
                dataSaver.setTransactionSuccessful();
                dataSaver.endTransaction();
                Log.d("DATA_SAVER", "records batch commited");

                dataSaver.beginTransaction();
                Log.d("DATA_SAVER", "started transaction");
            }
            if (importState.isTerminated()) break;
            try {
                dataSaver.saveRecordToDB(tableRows.getJSONObject(j));
            } catch (Exception e) {
                importState.appendMessage("Row not imported." + tableRows.getJSONObject(j));
                importState.appendMessage("Error: " + e.getClass().getSimpleName() + " - "
                                          + e.getMessage());
            }
            importState.incRowsProcessed();
        }
        dataSaver.setTransactionSuccessful();
        dataSaver.endTransaction();
        Log.d("DATA_SAVER", "remaining records batch commited");
    }

    private boolean needSaveBatch(int j) {
        return ((j + 1) % ROWS_IN_BATCH) == 0;
    }
}
