package ru.mmb.datacollector.transport.exporter.method;

import java.io.BufferedWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;
import ru.mmb.datacollector.transport.exporter.ExportState;

public class ExportDataMethodJson implements ExportDataMethod {
	private final ExportState exportState;
	private final DataExtractorFromDBToJson dataExtractor;
	private final BufferedWriter writer;

	public ExportDataMethodJson(ExportState exportState, DataExtractorFromDBToJson dataExtractor, BufferedWriter writer) {
		this.exportState = exportState;
		this.dataExtractor = dataExtractor;
		this.writer = writer;
	}

	@Override
	public void exportData(boolean exportWithRaw) throws Exception {
		JSONObject mainContainer = new JSONObject();
		if (exportState.isTerminated())
			return;
		if (exportWithRaw) {
			exportTable("RawLoggerData", mainContainer);
			if (exportState.isTerminated())
				return;
			exportTable("RawTeamLevelDismiss", mainContainer);
			if (exportState.isTerminated())
				return;
			exportTable("RawTeamLevelPoints", mainContainer);
			if (exportState.isTerminated())
				return;
		}
		exportTable("TeamLevelDismiss", mainContainer);
		if (exportState.isTerminated())
			return;
		exportTable("TeamLevelPoints", mainContainer);
		if (exportState.isTerminated())
			return;
		writer.write(mainContainer.toString());
	}

	private void exportTable(String tableName, JSONObject mainContainer) throws Exception {
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		table.setExportWhereAppendix("");
		JSONArray records = new JSONArray();
		dataExtractor.setTargetRecords(records);
		dataExtractor.setCurrentTable(table);
		if (dataExtractor.hasRecordsToExport()) {
			if (exportState.isTerminated())
				return;
			dataExtractor.exportNewRecords(exportState);
			mainContainer.put(tableName, records);
		}
	}
}
