package ru.mmb.terminal.transport.exporter.data;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.DataExtractor;
import ru.mmb.terminal.transport.exporter.DataExtractorToFile;
import ru.mmb.terminal.transport.exporter.DataExtractorToJson;
import ru.mmb.terminal.transport.exporter.ExportFormat;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import ru.mmb.terminal.util.DateFormat;

public class DataExporter
{
	private final Date exportDate;
	private final ExportMode exportMode;
	private final ExportState exportState;
	private final ExportFormat exportFormat;

	private BufferedWriter writer;

	public DataExporter(ExportMode exportMode, ExportState exportState, ExportFormat exportFormat)
	{
		this.exportDate = new Date();
		this.exportMode = exportMode;
		this.exportState = exportState;
		this.exportFormat = exportFormat;
	}

	public String exportData() throws Exception
	{
		String fileName = generateFileName(exportMode, exportDate);
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"));
		try
		{
			createExportDataMethod().exportData();
			updateLastExportDate();
		}
		finally
		{
			writer.close();
		}
		return fileName;
	}

	private ExportDataMethod createExportDataMethod() throws Exception
	{
		if (exportFormat == ExportFormat.TXT)
		{
			return new ExportDataMethodTxt(exportState, createDataExtractor(), writer);
		}
		else
		{
			return new ExportDataMethodJson(exportState, (DataExtractorToJson) createDataExtractor(), writer);
		}
	}

	private DataExtractor createDataExtractor()
	{
		if (exportFormat == ExportFormat.TXT)
		{
			return new DataExtractorToFile(exportMode, writer);
		}
		else
		{
			return new DataExtractorToJson(exportMode);
		}
	}

	private String generateFileName(ExportMode exportMode, Date exportDate)
	{
		String result = Settings.getInstance().getExportDir() + "/exp_";
		result +=
		    Settings.getInstance().getUserId() + "_" + Settings.getInstance().getDeviceId() + "_"
		            + exportMode.getShortName() + "_" + DateFormat.format(exportDate) + "."
		            + exportFormat.getFileExtension();
		return result;
	}

	private void updateLastExportDate()
	{
		if (exportMode == ExportMode.INCREMENTAL)
		{
			Settings.getInstance().setLastExportDate(DateFormat.format(exportDate));
		}
	}
}
