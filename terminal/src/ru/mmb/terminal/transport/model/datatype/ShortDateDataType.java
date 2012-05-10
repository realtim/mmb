package ru.mmb.terminal.transport.model.datatype;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.util.DateFormat;
import ru.mmb.terminal.util.TransportDateFormat;
import android.database.Cursor;

public class ShortDateDataType extends DataType<Date>
{
	@Override
	public Date decodeJSON(String columnName, JSONObject tableRow) throws JSONException
	{
		return TransportDateFormat.parseShort(tableRow.getString(columnName));
	}

	@Override
	public String encodeString(Object value)
	{
		return TransportDateFormat.formatShort((Date) value);
	}

	@Override
	public Date getFromDB(Cursor cursor, int columnIndex)
	{
		return DateFormat.parse(cursor.getString(columnIndex));
	}

	@Override
	public String encodeToDB(Object value)
	{
		Date dateValue = (Date) value;
		return "'" + DateFormat.format(dateValue) + "'";
	}
}
