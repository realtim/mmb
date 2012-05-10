package ru.mmb.terminal.transport.importer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.transport.registry.MetaTablesRegistry;

public class Importer
{
	private InputStreamReader reader;
	private final ImportState importState;

	public Importer(ImportState importState)
	{
		this.importState = importState;
	}

	public void importPackage(String fileName) throws IOException, FileNotFoundException,
	        JSONException
	{
		reader = new InputStreamReader(new FileInputStream(fileName), "UTF8");

		importState.appendMessage("Import started.");

		String jsonString = readFromFile();
		JSONObject tables = new JSONObject(jsonString);

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
			JSONArray tableRows = tables.getJSONArray(tableName);
			resetImportState(metaTable, tableRows.length());
			for (int j = 0; j < tableRows.length(); j++)
			{
				if (importState.isTerminated()) break;
				try
				{
					dataSaver.saveRecordToDB(tableRows.getJSONObject(j));
				}
				catch (Exception e)
				{
					importState.appendMessage("Row not imported. " + tableRows.getJSONObject(j));
					importState.appendMessage("Error: " + e.getClass().getSimpleName() + " - "
					        + e.getMessage());
				}
				importState.incRowsProcessed();
			}
		}
	}

	private String readFromFile() throws IOException
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[32768];

			int charsRead = reader.read(buffer);
			while (charsRead != -1)
			{
				sb.append(buffer, 0, charsRead);
				charsRead = reader.read(buffer);
			}
			return sb.toString();
		}
		finally
		{
			reader.close();
		}
	}

	private MetaTable getMetaTable(String tableName)
	{
		return MetaTablesRegistry.getInstance().getTableByName(tableName);
	}

	private void resetImportState(MetaTable metaTable, int totalRows)
	{
		importState.setCurrentTable(metaTable.getTableName());
		importState.setTotalRows(totalRows);
		importState.setRowsProcessed(0);
	}
}
