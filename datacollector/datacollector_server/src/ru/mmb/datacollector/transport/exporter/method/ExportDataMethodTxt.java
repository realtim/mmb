package ru.mmb.datacollector.transport.exporter.method;

import java.io.BufferedWriter;
import java.io.IOException;

import ru.mmb.datacollector.conf.Settings;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;
import ru.mmb.datacollector.transport.exporter.ExportState;

public class ExportDataMethodTxt implements ExportDataMethod {
	private final ExportState exportState;
	private final DataExtractorFromDB dataExtractor;
	private final BufferedWriter writer;

	public ExportDataMethodTxt(ExportState exportState, DataExtractorFromDB dataExtractor, BufferedWriter writer) {
		this.exportState = exportState;
		this.dataExtractor = dataExtractor;
		this.writer = writer;
	}

	@Override
	public void exportData() throws Exception {
		writeHeader();
		if (exportState.isTerminated())
			return;
		exportTable("TeamLevelDismiss");
		if (exportState.isTerminated())
			return;
		exportTable("TeamLevelPoints");
		if (exportState.isTerminated())
			return;
		writeFooter();
	}

	private void writeHeader() throws IOException {
		writer.write(Settings.getInstance().getTransportUserId());
		writer.newLine();
		writer.write(Settings.getInstance().getTransportUserPassword());
		writer.newLine();
	}

	private void exportTable(String tableName) throws Exception {
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		table.setExportWhereAppendix("");
		dataExtractor.setCurrentTable(table);
		if (dataExtractor.hasRecordsToExport()) {
			if (exportState.isTerminated())
				return;
			writer.write("---" + tableName);
			writer.newLine();
			dataExtractor.exportNewRecords(exportState);
		}
	}

	private void writeFooter() throws IOException {
		writer.write("end");
		writer.newLine();
	}
}
