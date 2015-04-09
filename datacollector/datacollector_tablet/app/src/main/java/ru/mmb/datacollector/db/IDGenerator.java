package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class IDGenerator
{
	private static final String TABLE_LOCAL_SEQUENCE = "LocalSequence";
	private static final String SEQUENCE_ID = "sequence_id";
	private static final String SEQUENCE_VALUE = "sequence_value";

	private static final int ID_INCREMENT = 100;

	private final SQLiteDatabase db;
	private int currentId;
	private int maxId;

	public IDGenerator(SQLiteDatabase db)
	{
		this.db = db;
		// TODO uncomment when needed
		// init();
	}

	@SuppressWarnings("unused")
	private void init()
	{
		String whereCondition = SEQUENCE_ID + " = 1";
		Cursor resultCursor =
		    db.query(TABLE_LOCAL_SEQUENCE, new String[] { SEQUENCE_VALUE }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		currentId = resultCursor.getInt(0);
		maxId = currentId + ID_INCREMENT;
		resultCursor.close();
		updateSequenceValue();
	}

	public int getNextId()
	{
		currentId++;
		int result = currentId;
		if (currentId == maxId)
		{
			updateSequenceValue();
		}
		return result;
	}

	private void updateSequenceValue()
	{
		db.beginTransaction();
		try
		{
			maxId += ID_INCREMENT;
			String sql =
			    "update " + TABLE_LOCAL_SEQUENCE + " set " + SEQUENCE_VALUE + " = " + maxId
			            + " where " + SEQUENCE_ID + " = 1";
			db.execSQL(sql);
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}
}
