package ru.mmb.terminal.transport.importer.barcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.transport.importer.Importer;

public class BarcodeFileReader
{
	private final String fileName;
	private final int scanPointId;

	public BarcodeFileReader(String fileName, int scanPointId)
	{
		this.fileName = fileName;
		this.scanPointId = scanPointId;
	}

	public JSONObject readBarCodeScanData() throws IOException, JSONException
	{
		BufferedReader reader =
		    new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "US-ASCII"));
		try
		{
			JSONArray records = new JSONArray();
			String inputLine = reader.readLine();
			while (inputLine != null)
			{
				addLineToJSONArray(inputLine, records);
				inputLine = reader.readLine();
			}
			JSONObject mainContainer = new JSONObject();
			mainContainer.put(Importer.TABLE_BAR_CODE_SCANS, records);
			return mainContainer;
		}
		finally
		{
			reader.close();
		}
	}

	private void addLineToJSONArray(String inputLine, JSONArray records)
	{
		// TODO Parse line to objects: scanPointId, teamId, readDate.
		// TODO Create JSON object using this data.
		// TODO Append JSON to result array.
	}
}
