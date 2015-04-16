package ru.mmb.datacollector.model.meta.datatype;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class DataType<T> {
	abstract public T decodeJSON(String columnName, JSONObject tableRow) throws JSONException;

	abstract public void appendToJSON(JSONObject targetJSON, String columnName, Object value) throws JSONException;

	abstract public String encodeString(Object value);

	abstract public T getFromDB(ResultSet rs, int columnIndex) throws SQLException;

	abstract public String encodeToDB(Object value);
}
