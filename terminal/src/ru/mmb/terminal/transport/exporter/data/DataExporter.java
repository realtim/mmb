package ru.mmb.terminal.transport.exporter.data;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.DataExtractorToFile;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.transport.registry.MetaTablesRegistry;
import ru.mmb.terminal.util.DateFormat;

public class DataExporter
{
	private final Date exportDate;
	private final ExportMode exportMode;
	private final ExportState exportState;

	private DataExtractorToFile dataExtractor;
	private BufferedWriter writer;

	public DataExporter(ExportMode exportMode, ExportState exportState)
	{
		this.exportDate = new Date();
		this.exportMode = exportMode;
		this.exportState = exportState;
	}

	public String exportData() throws Exception
	{
		String fileName = generateFileName(exportMode, exportDate);
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
		dataExtractor = new DataExtractorToFile(exportMode, writer);
		try
		{
			writeHeader();
			if (exportState.isTerminated()) return "";
			exportTable("TeamLevelDismiss");
			if (exportState.isTerminated()) return "";
			exportTable("TeamLevelPoints");
			if (exportState.isTerminated()) return "";
			writeFooter();
			updateLastExportDate();
		}
		finally
		{
			writer.close();
		}
		return fileName;
	}

	private String generateFileName(ExportMode exportMode, Date exportDate)
	{
		String result = Settings.getInstance().getExportDir() + "/exp_";
		result +=
		    Settings.getInstance().getUserId() + "_" + Settings.getInstance().getDeviceId() + "_"
		            + exportMode.getShortName() + "_" + DateFormat.format(exportDate) + ".txt";
		return result;
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

	private void updateLastExportDate()
	{
		if (exportMode == ExportMode.INCREMENTAL)
		{
			Settings.getInstance().setLastExportDate(DateFormat.format(exportDate));
		}
	}
}
