package ru.mmb.datacollector.transport.exporter.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.DataExtractorToJson;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.model.MetaTable;
import ru.mmb.datacollector.transport.registry.MetaTablesRegistry;

public class ExportDataMethodJson implements ExportDataMethod
{
	private final ExportState exportState;
	private final DataExtractorToJson dataExtractor;
	private final BufferedWriter writer;

	public ExportDataMethodJson(ExportState exportState, DataExtractorToJson dataExtractor, BufferedWriter writer)
	{
		this.exportState = exportState;
		this.dataExtractor = dataExtractor;
		this.writer = writer;
	}

	@Override
	public void exportData() throws Exception
	{
		JSONObject mainContainer = new JSONObject();
        if (exportState.isTerminated()) return;
        exportTable("RawLoggerData", mainContainer);
		if (exportState.isTerminated()) return;
		exportTable("RawTeamLevelDismiss", mainContainer);
		if (exportState.isTerminated()) return;
		exportTable("RawTeamLevelPoints", mainContainer);
		if (exportState.isTerminated()) return;
		writer.write(mainContainer.toString());
	}

	private void exportTable(String tableName, JSONObject mainContainer) throws Exception
	{
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		table.setExportWhereAppendix("");
		table.setLastExportDate(Settings.getInstance().getLastExportDate());
		JSONArray records = new JSONArray();
		dataExtractor.setTargetRecords(records);
		dataExtractor.setCurrentTable(table);
		if (dataExtractor.hasRecordsToExport())
		{
			if (exportState.isTerminated()) return;
			dataExtractor.exportNewRecords(exportState);
			mainContainer.put(tableName, records);
		}
	}
}
