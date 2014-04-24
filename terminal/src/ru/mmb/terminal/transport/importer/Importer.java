package ru.mmb.terminal.transport.importer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.transport.importer.barcode.BarcodeFileReader;
import ru.mmb.terminal.transport.model.ImportBarCodeMetaTable;
import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.transport.registry.MetaTablesRegistry;
import ru.mmb.terminal.util.JSONUtils;
import android.util.Log;

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

	public void importPackage(String fileName) throws IOException, FileNotFoundException,
	        JSONException
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
			tables = readBarCodeScanData(fileName, scanPoint.getScanPointId());
		}
		else
		{
			tables = readJsonTablesPackage(fileName);
		}

		importState.appendMessage("File loaded to JSON object.");

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

	private JSONObject readBarCodeScanData(String fileName, int scanPointId) throws IOException,
	        JSONException
	{
		return new BarcodeFileReader(fileName, scanPointId).readBarCodeScanData();
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
