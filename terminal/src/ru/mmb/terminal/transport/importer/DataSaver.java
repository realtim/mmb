package ru.mmb.terminal.transport.importer;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.transport.model.MetaTable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DataSaver
{
	private MetaTable currentTable = null;
	private final SQLiteDatabase db;

	public DataSaver()
	{
		db = TerminalDB.getInstance().getDb();
	}

	public void setCurrentTable(MetaTable metaTable)
	{
		currentTable = metaTable;
	}

	public void saveRecordToDB(JSONObject tableRow) throws JSONException
	{
		if (currentTable == null) return;
		if (tableRow == null) return;

		ImportToDBAction action = getImportToDBAction(tableRow);
		if (action == ImportToDBAction.UPDATE)
		{
			updateRecord(tableRow);
		}
		else if (action == ImportToDBAction.INSERT)
		{
			insertRecord(tableRow);
		}
	}

	private ImportToDBAction getImportToDBAction(JSONObject tableRow) throws JSONException
	{
		if (currentTable.getUpdateDateColumnName() == null)
		{
			if (isRecordExists(tableRow))
			{
				return ImportToDBAction.UPDATE;
			}
			else
			{
				return ImportToDBAction.INSERT;
			}
		}
		else
		{
			Date recordUpdateDate = getRecordUpdateDate(tableRow);
			if (recordUpdateDate == null)
			{
				// record doesn't exist, insert needed
				return ImportToDBAction.INSERT;
			}
			Date bufferUpdateDate = currentTable.getUpdateDate(tableRow);
			if (recordUpdateDate.before(bufferUpdateDate))
			{
				return ImportToDBAction.UPDATE;
			}
			else
			{
				return ImportToDBAction.IGNORE;
			}
		}
	}

	private boolean isRecordExists(JSONObject tableRow) throws JSONException
	{
		String sql = currentTable.generateCheckExistsSQL(tableRow);
		Cursor cursor = db.rawQuery(sql, null);
		try
		{
			cursor.moveToFirst();
			return cursor.getInt(0) == 1;
		}
		finally
		{
			cursor.close();
		}
	}

	private Date getRecordUpdateDate(JSONObject tableRow) throws JSONException
	{
		Date result = null;
		String sql = currentTable.generateUpdateDateSelectSQL(tableRow);
		Cursor cursor = db.rawQuery(sql, null);
		try
		{
			cursor.moveToFirst();
			if (cursor.getCount() == 0) return null;
			result = (Date) currentTable.getUpdateDateColumn().getValue(cursor, 0);
			return result;
		}
		finally
		{
			cursor.close();
		}
	}

	private void updateRecord(JSONObject tableRow) throws JSONException
	{
		String sql = currentTable.generateUpdateSQL(tableRow);
		db.execSQL(sql);
	}

	private void insertRecord(JSONObject tableRow) throws JSONException
	{
		String sql = currentTable.generateInsertSQL(tableRow);
		db.execSQL(sql);
	}
}
