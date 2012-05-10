package ru.mmb.terminal.transport.model.datatype;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

public class IntegerDataType extends DataType<Integer>
{
	@Override
	public Integer decodeJSON(String columnName, JSONObject tableRow) throws JSONException
	{
		return tableRow.getInt(columnName);
	}

	@Override
	public String encodeString(Object value)
	{
		return ((Integer) value).toString();
	}

	@Override
	public Integer getFromDB(Cursor cursor, int columnIndex)
	{
		return cursor.getInt(columnIndex);
	}

	@Override
	public String encodeToDB(Object value)
	{
		Integer intValue = (Integer) value;
		return Integer.toString(intValue);
	}
}
