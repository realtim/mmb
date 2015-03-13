package ru.mmb.datacollector.transport.exporter;

import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.transport.model.MetaTable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class DataExtractor
{
	private MetaTable currentTable = null;
	private final SQLiteDatabase db;
	private final ExportMode exportMode;

	protected abstract void exportRow(Cursor cursor) throws Exception;

	public DataExtractor(ExportMode exportMode)
	{
		// DatacollectorDB.getRawInstance() will never be null, but db can be null.
		this.db = DatacollectorDB.getRawInstance().getDb();
		this.exportMode = exportMode;
	}

	public void setCurrentTable(MetaTable metaTable)
	{
		currentTable = metaTable;
	}

	public MetaTable getCurrentTable()
	{
		return currentTable;
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

	public void exportNewRecords(ExportState exportState) throws Exception
	{
		String selectSql = currentTable.generateSelectNewRecordsSQL(exportMode);
		Cursor cursor = db.rawQuery(selectSql, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast() && !exportState.isTerminated())
		{
			exportRow(cursor);
			cursor.moveToNext();
		}
		cursor.close();
	}
}
