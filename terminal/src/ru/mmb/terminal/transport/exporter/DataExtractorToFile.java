package ru.mmb.terminal.transport.exporter;

import java.io.BufferedWriter;

import android.database.Cursor;

public class DataExtractorToFile extends DataExtractor
{
	private final BufferedWriter writer;

	public DataExtractorToFile(ExportMode exportMode, BufferedWriter writer)
	{
		super(exportMode);
		this.writer = writer;
	}

	@Override
	protected void exportRow(Cursor cursor) throws Exception
	{
		String rowToExport = null;
		try
		{
			rowToExport = getCurrentTable().generateExportRowString(cursor);
		}
		catch (Exception e)
		{
		}
		if (rowToExport != null)
		{
			writer.write(rowToExport);
			writer.newLine();
		}
	}
}
