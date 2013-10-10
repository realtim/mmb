package ru.mmb.terminal.transport.exporter;

import org.json.JSONArray;
import org.json.JSONObject;

import android.database.Cursor;

public class DataExtractorToJSON extends DataExtractor
{
	private final JSONArray records;

	public DataExtractorToJSON(ExportMode exportMode, JSONArray records)
	{
		super(exportMode);
		this.records = records;
	}

	@Override
	protected void exportRow(Cursor cursor) throws Exception
	{
		JSONObject rowJSON = getCurrentTable().generateExportRowJSON(cursor);
		records.put(rowJSON);
	}
}
