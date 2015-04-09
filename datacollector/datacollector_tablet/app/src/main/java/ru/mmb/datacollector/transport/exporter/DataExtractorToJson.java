package ru.mmb.datacollector.transport.exporter;

import org.json.JSONArray;
import org.json.JSONObject;

import android.database.Cursor;

public class DataExtractorToJson extends DataExtractor
{
	private JSONArray records;

	public DataExtractorToJson(ExportMode exportMode)
	{
		super(exportMode);
	}

	public void setTargetRecords(JSONArray records)
	{
		this.records = records;
	}

	@Override
	protected void exportRow(Cursor cursor) throws Exception
	{
		JSONObject rowJSON = getCurrentTable().generateExportRowJSON(cursor);
		records.put(rowJSON);
	}
}
