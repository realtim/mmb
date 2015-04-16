package ru.mmb.datacollector.model.meta.datatype;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.util.DateFormat;
import ru.mmb.datacollector.util.TransportDateFormat;

public class LongDateDataType extends DataType<Date> {
	@Override
	public Date decodeJSON(String columnName, JSONObject tableRow) throws JSONException {
		return TransportDateFormat.parseLong(tableRow.getString(columnName));
	}

	@Override
	public String encodeString(Object value) {
		return TransportDateFormat.formatLong((Date) value);
	}

	@Override
	public Date getFromDB(ResultSet rs, int columnIndex) throws SQLException {
		return DateFormat.parse(rs.getString(columnIndex));
	}

	@Override
	public String encodeToDB(Object value) {
		Date dateValue = (Date) value;
		return "'" + DateFormat.format(dateValue) + "'";
	}

	@Override
	public void appendToJSON(JSONObject targetJSON, String columnName, Object value) throws JSONException {
		if (value == null)
			targetJSON.put(columnName, JSONObject.NULL);
		else
			targetJSON.put(columnName, encodeString(value));
	}
}
