package ru.mmb.terminal.db;

import java.util.Properties;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Settings
{
	private static final String TABLE_SETTINGS = "Settings";

	private static final String SETTING_NAME = "setting_name";
	private static final String SETTING_VALUE = "setting_value";

	private final SQLiteDatabase db;

	public Settings(SQLiteDatabase db)
	{
		this.db = db;
	}

	public Properties loadSettings()
	{
		Properties result = new Properties();

		Cursor resultCursor =
		    db.query(TABLE_SETTINGS, new String[] { SETTING_NAME, SETTING_VALUE }, null, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			String settingName = resultCursor.getString(0);
			String settingValue = null;
			if (!resultCursor.isNull(1))
			{
				settingValue = resultCursor.getString(1);
				result.put(settingName, settingValue);
			}

			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public void setSettingValue(String settingName, String settingValue)
	{
		db.beginTransaction();
		try
		{
			String updateSql =
			    "update " + TABLE_SETTINGS + " set " + SETTING_VALUE + " = "
			            + getValueString(settingValue) + " where " + SETTING_NAME + " = '"
			            + settingName + "'";
			db.execSQL(updateSql);

			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	private String getValueString(String settingValue)
	{
		if (settingValue == null) return "null";
		return "'" + settingValue.replace("'", "''") + "'";
	}
}
