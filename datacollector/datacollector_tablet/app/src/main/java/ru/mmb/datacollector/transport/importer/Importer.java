package ru.mmb.datacollector.transport.importer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.transport.importer.barcode.BarcodeFileReader;
import ru.mmb.datacollector.model.meta.ImportBarCodeMetaTable;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;
import ru.mmb.datacollector.util.JSONUtils;

public class Importer
{
	public static final String TABLE_BAR_CODE_SCANS = "BarCodeScans";

	private static final int ROWS_IN_BATCH = 200;

	private final ImportState importState;
	private final ScanPoint scanPoint;

	public Importer(ImportState importState, ScanPoint scanPoint)
	{
		this.importState = importState;
		this.scanPoint = scanPoint;
	}

	public void importPackageFromFile(String fileName) throws Exception
	{
		importState.appendMessage("Import started.");

		JSONObject tables;
		if (fileName.toUpperCase().endsWith(".TXT"))
		{
			if (scanPoint == null)
			{
				importState.appendMessage("Import failed. ScanPoint for barcode scans import not defined.");
				return;
			}
			tables = readBarCodeScanData(fileName);
		}
		else
		{
			tables = readJsonTablesPackage(fileName);
		}

		importState.appendMessage("File loaded to JSON object.");

		importPackageFromJsonObject(tables);
	}

    public void importPackageFromJsonObject(JSONObject tables) throws Exception {
        DataSaver dataSaver = new DataSaver();
        JSONArray names = tables.names();
        for (int i = 0; i < names.length(); i++)
        {
            if (importState.isTerminated()) break;
            String tableName = names.getString(i);
            importState.appendMessage("Importing table: " + tableName);
            MetaTable metaTable = getMetaTable(tableName);
            if (metaTable == null)
            {
                importState.appendMessage("Meta table not found.");
                continue;
            }
            dataSaver.setCurrentTable(metaTable);
            if (metaTable.needClearBeforeImport())
            {
                dataSaver.clearCurrentTable();
                Log.d("data saver", "table cleared: " + tableName);
            }
            JSONArray tableRows = tables.getJSONArray(tableName);
            resetImportState(metaTable, tableRows.length());
            importTableRows(dataSaver, tableRows);
        }
    }

	private JSONObject readBarCodeScanData(String fileName) throws Exception
	{
		return new BarcodeFileReader(fileName, scanPoint, importState).readBarCodeScanData();
	}

	private JSONObject readJsonTablesPackage(String fileName) throws IOException, JSONException
	{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(fileName), "UTF8");
		String jsonString = JSONUtils.readFromInputStream(reader, 32768);
		return new JSONObject(jsonString);
	}

	private MetaTable getMetaTable(String tableName)
	{
		if (TABLE_BAR_CODE_SCANS.equals(tableName))
			return new ImportBarCodeMetaTable();
		else
			return MetaTablesRegistry.getInstance().getTableByName(tableName);
	}

	private void resetImportState(MetaTable metaTable, int totalRows)
	{
		importState.setCurrentTable(metaTable.getTableName());
		importState.setTotalRows(totalRows);
		importState.setRowsProcessed(0);
	}

	private void importTableRows(DataSaver dataSaver, JSONArray tableRows) throws JSONException
	{
		dataSaver.beginTransaction();
		Log.d("data saver", "started first transaction");
		for (int j = 0; j < tableRows.length(); j++)
		{
			if (needSaveBatch(j))
			{
				dataSaver.setTransactionSuccessful();
				dataSaver.endTransaction();
				Log.d("data saver", "records batch commited");

				dataSaver.beginTransaction();
				Log.d("data saver", "started transaction");
			}
			if (importState.isTerminated()) break;
			try
			{
				dataSaver.saveRecordToDB(tableRows.getJSONObject(j));
			}
			catch (Exception e)
			{
				importState.appendMessage("Row not imported." + tableRows.getJSONObject(j));
				importState.appendMessage("Error: " + e.getClass().getSimpleName() + " - "
				        + e.getMessage());
			}
			importState.incRowsProcessed();
		}
		dataSaver.setTransactionSuccessful();
		dataSaver.endTransaction();
		Log.d("data saver", "remaining records batch commited");
	}

	private boolean needSaveBatch(int j)
	{
		return ((j + 1) % ROWS_IN_BATCH) == 0;
	}
}
