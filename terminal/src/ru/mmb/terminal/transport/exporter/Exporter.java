package ru.mmb.terminal.transport.exporter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.transport.registry.MetaTablesRegistry;
import ru.mmb.terminal.util.DateFormat;
import ru.mmb.terminal.util.ExternalStorage;

public class Exporter
{
	private final Date exportDate;
	private final ExportMode exportMode;
	private final DataExtracter dataExtracter;

	private BufferedWriter writer;

	public Exporter(ExportMode exportMode)
	{
		this.exportDate = new Date();
		this.exportMode = exportMode;
		this.dataExtracter = new DataExtracter(exportMode);
	}

	public String exportData() throws Exception
	{
		String fileName = generateFileName(exportMode, exportDate);
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
		try
		{
			writeHeader();
			exportTable("TeamLevelDismiss");
			exportTable("TeamLevelPoints");
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
		String result = ExternalStorage.getDir() + "/mmb/export/terminal_export_";
		result +=
		    Settings.getInstance().getUserId() + "_" + Settings.getInstance().getDeviceId() + "_"
		            + exportMode.name() + "_" + DateFormat.format(exportDate) + ".txt";
		return result;
	}

	private void writeHeader() throws IOException
	{
		writer.write(Integer.toString(Settings.getInstance().getTranspUserId()));
		writer.newLine();
		writer.write(Settings.getInstance().getTranspUserPassword());
		writer.newLine();
	}

	private void exportTable(String tableName) throws IOException
	{
		MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
		dataExtracter.setCurrentTable(table);
		if (dataExtracter.hasRecordsToExport())
		{
			writer.write("---" + tableName);
			writer.newLine();
			dataExtracter.exportNewRecordsToFile(writer);
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
