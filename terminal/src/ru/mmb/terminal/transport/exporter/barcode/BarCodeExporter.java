package ru.mmb.terminal.transport.exporter.barcode;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.DataExtractorToJSON;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.transport.registry.MetaTablesRegistry;
import ru.mmb.terminal.util.DateFormat;

public class BarCodeExporter
{
	private static final String TABLE_BAR_CODE_SCANS = "BarCodeScans";
	private static final String LEVELPOINT_ID = "levelpoint_id";

	private final Date exportDate;
	private final ExportMode exportMode;
	private final ExportState exportState;
	private final Integer levelPointId;

	public BarCodeExporter(ExportMode exportMode, ExportState exportState, int levelPointId)
	{
		this.exportDate = new Date();
		this.exportMode = exportMode;
		this.exportState = exportState;
		this.levelPointId = new Integer(levelPointId);
	}

	public String exportData() throws Exception
	{
		String fileName = generateFileName();

		JSONArray records = new JSONArray();
		exportTable(TABLE_BAR_CODE_SCANS, records);
		JSONObject mainContainer = new JSONObject();
		mainContainer.put(TABLE_BAR_CODE_SCANS, records);

		writeJSONToFile(fileName, mainContainer.toString());
		return fileName;
	}

	private String generateFileName()
	{
		String result = Settings.getInstance().getExportDir() + "/barcode_exp_";
		result +=
		    Settings.getInstance().getUserId() + "_" + Settings.getInstance().getDeviceId() + "_"
		            + levelPointId + "_" + exportMode.getShortName() + "_"
		            + DateFormat.format(exportDate) + ".json";
		return result;
	}

	private void exportTable(String tableName, JSONArray records) throws Exception
	{
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		table.setExportWhereAppendix(LEVELPOINT_ID + " = " + levelPointId);
		table.setLastExportDate(Settings.getInstance().getBarCodeLastExportDate(levelPointId));
		DataExtractorToJSON dataExtractor = new DataExtractorToJSON(exportMode, records);
		dataExtractor.setCurrentTable(table);
		if (dataExtractor.hasRecordsToExport())
		{
			if (exportState.isTerminated()) return;
			dataExtractor.exportNewRecords(exportState);
		}
	}

	private void writeJSONToFile(String fileName, String jsonString)
	        throws UnsupportedEncodingException, FileNotFoundException, IOException
	{
		BufferedWriter writer =
		    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
		try
		{
			writer.write(jsonString);
			updateLastExportDate();
		}
		finally
		{
			writer.close();
		}
	}

	private void updateLastExportDate()
	{
		if (exportMode == ExportMode.INCREMENTAL)
		{
			Settings.getInstance().setBarCodeLastExportDate(levelPointId, DateFormat.format(exportDate));
		}
	}
}
