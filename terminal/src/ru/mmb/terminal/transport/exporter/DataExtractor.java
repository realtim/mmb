package ru.mmb.terminal.transport.exporter;

import java.io.BufferedWriter;
import java.io.IOException;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.transport.model.MetaTable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DataExtractor
{
	private MetaTable currentTable = null;
	private final SQLiteDatabase db;
	private final ExportMode exportMode;

	public DataExtractor(ExportMode exportMode)
	{
		this.db = TerminalDB.getInstance().getDb();
		this.exportMode = exportMode;
	}

	public void setCurrentTable(MetaTable metaTable)
	{
		currentTable = metaTable;
	}

	public boolean hasRecordsToExport()
	{
		if (currentTable == null) return false;

		String sql = currentTable.generateCheckNewRecordsSQL(exportMode);
		Cursor cursor = db.rawQuery(sql, null);
		try
		{
			cursor.moveToFirst();
			return cursor.getInt(0) > 0;
		}
		finally
		{
			cursor.close();
		}
	}

	public void exportNewRecordsToFile(BufferedWriter writer, ExportState exportState)
	        throws IOException
	{
		String selectSql = currentTable.generateSelectNewRecordsSQL(exportMode);
		Cursor cursor = db.rawQuery(selectSql, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast() && !exportState.isTerminated())
		{
			exportRow(writer, cursor);
			cursor.moveToNext();
		}
		cursor.close();
	}

	private void exportRow(BufferedWriter writer, Cursor cursor) throws IOException
	{
		String rowToExport = null;
		try
		{
			rowToExport = currentTable.generateExportRowString(cursor);
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
