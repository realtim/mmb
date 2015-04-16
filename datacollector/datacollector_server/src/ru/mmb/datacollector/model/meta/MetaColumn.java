package ru.mmb.datacollector.model.meta;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.model.meta.datatype.DataType;

public class MetaColumn {
	public static final String NULL = "NULL";

	private final int columnId;
	private final int tableId;
	private final String columnName;
	private final int columnOrder;
	private final DataType<? extends Object> columnDataType;
	private final boolean primaryKey;

	private MetaTable table;

	public MetaColumn(int columnId, int tableId, String columnName, int columnOrder,
			DataType<? extends Object> columnDataType, boolean primaryKey) {
		this.columnId = columnId;
		this.tableId = tableId;
		this.columnName = columnName;
		this.columnOrder = columnOrder;
		this.columnDataType = columnDataType;
		this.primaryKey = primaryKey;
	}

	public MetaTable getTable() {
		return table;
	}

	public void setTable(MetaTable table) {
		this.table = table;
	}

	public int getColumnId() {
		return columnId;
	}

	public int getTableId() {
		return tableId;
	}

	public String getColumnName() {
		return columnName;
	}

	public int getColumnOrder() {
		return columnOrder;
	}

	public DataType<? extends Object> getColumnDataType() {
		return columnDataType;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public Object getValue(JSONObject tableRow) throws JSONException {
		if (isNull(tableRow))
			return null;
		return columnDataType.decodeJSON(columnName, tableRow);
	}

	public void appendToJSON(JSONObject targetJSON, Object value) throws JSONException {
		columnDataType.appendToJSON(targetJSON, columnName, value);
	}

	private boolean isNull(JSONObject tableRow) throws JSONException {
		return tableRow.isNull(columnName) || tableRow.get(columnName) == null;
	}

	public String decorateForDBQuery(JSONObject tableRow) throws JSONException {
		Object value = getValue(tableRow);
		if (value == null)
			return NULL;
		return columnDataType.encodeToDB(value);
	}

	public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
		Object result = columnDataType.getFromDB(rs, columnIndex);
		if (rs.wasNull()) {
			return null;
		}
		return result;
	}

	public String decorateForExportToString(ResultSet rs) throws SQLException {
		Object value = getValue(rs, columnOrder);
		if (value == null) {
			return "\"" + NULL + "\"";
		}
		return "\"" + columnDataType.encodeString(value) + "\"";
	}

	public Object decorateForExportToJSON(ResultSet rs) throws SQLException {
		Object value = getValue(rs, columnOrder);
		if (value == null)
			return JSONObject.NULL;
		return columnDataType.encodeString(value);
	}
}
