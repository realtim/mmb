package ru.mmb.datacollector.transport.model.datatype;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

public class TextDataType extends DataType<String>
{
	@Override
	public String decodeJSON(String columnName, JSONObject tableRow) throws JSONException
	{
		return tableRow.getString(columnName);
	}

	@Override
	public String encodeString(Object value)
	{
		return (String) value;
	}

	@Override
	public String getFromDB(Cursor cursor, int columnIndex)
	{
		return cursor.getString(columnIndex);
	}

	@Override
	public String encodeToDB(Object value)
	{
		String stringValue = (String) value;
		String prepared = stringValue.replace("'", "''");
		return "'" + prepared + "'";
	}

	@Override
	public void appendToJSON(JSONObject targetJSON, String columnName, Object value)
	        throws JSONException
	{
		if (value == null)
			targetJSON.put(columnName, JSONObject.NULL);
		else
			targetJSON.put(columnName, value);
	}
}
