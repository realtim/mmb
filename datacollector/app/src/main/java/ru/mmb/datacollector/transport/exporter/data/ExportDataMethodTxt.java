package ru.mmb.datacollector.transport.exporter.data;

import java.io.BufferedWriter;
import java.io.IOException;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.DataExtractor;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.transport.model.MetaTable;
import ru.mmb.datacollector.transport.registry.MetaTablesRegistry;

public class ExportDataMethodTxt implements ExportDataMethod
{
	private final ExportState exportState;
	private final DataExtractor dataExtractor;
	private final BufferedWriter writer;

	public ExportDataMethodTxt(ExportState exportState, DataExtractor dataExtractor, BufferedWriter writer)
	{
		this.exportState = exportState;
		this.dataExtractor = dataExtractor;
		this.writer = writer;
	}

	public void exportData() throws Exception
	{
		writeHeader();
		if (exportState.isTerminated()) return;
		exportTable("TeamLevelDismiss");
		if (exportState.isTerminated()) return;
		exportTable("TeamLevelPoints");
		if (exportState.isTerminated()) return;
		writeFooter();
	}

	private void writeHeader() throws IOException
	{
		writer.write(Integer.toString(Settings.getInstance().getTranspUserId()));
		writer.newLine();
		writer.write(Settings.getInstance().getTranspUserPassword());
		writer.newLine();
	}

	private void exportTable(String tableName) throws Exception
	{
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		table.setExportWhereAppendix("");
		table.setLastExportDate(Settings.getInstance().getLastExportDate());
		dataExtractor.setCurrentTable(table);
		if (dataExtractor.hasRecordsToExport())
		{
			if (exportState.isTerminated()) return;
			writer.write("---" + tableName);
			writer.newLine();
			dataExtractor.exportNewRecords(exportState);
		}
	}

	private void writeFooter() throws IOException
	{
		writer.write("end");
		writer.newLine();
	}
}
