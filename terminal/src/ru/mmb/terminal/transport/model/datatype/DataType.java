package ru.mmb.terminal.transport.model.datatype;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

public abstract class DataType<T>
{
	abstract public T decodeJSON(String columnName, JSONObject tableRow) throws JSONException;

	abstract public String encodeString(Object value);

	abstract public T getFromDB(Cursor cursor, int columnIndex);

	abstract public String encodeToDB(Object value);
}
