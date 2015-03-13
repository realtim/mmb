package ru.mmb.datacollector.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.transport.model.MetaColumn;
import ru.mmb.datacollector.transport.model.MetaTable;
import ru.mmb.datacollector.transport.model.datatype.DataType;
import ru.mmb.datacollector.transport.model.datatype.DataTypes;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MetaTables
{
	private static final String TABLE_META_TABLES = "MetaTables";
	private static final String TABLE_META_COLUMNS = "MetaColumns";

	private static final String TABLE_ID = "table_id";
	private static final String TABLE_NAME = "table_name";
	private static final String UPDATE_DATE_COLUMN_NAME = "update_date_column_name";
	private static final String COLUMN_ID = "column_id";
	private static final String COLUMN_NAME = "column_name";
	private static final String COLUMN_ORDER = "column_order";
	private static final String COLUMN_DATA_TYPE = "column_data_type";
	private static final String IS_PRIMARY_KEY = "is_primary_key";

	private final SQLiteDatabase db;

	public MetaTables(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<MetaTable> loadMetaTables()
	{
		List<MetaTable> result = new ArrayList<MetaTable>();
		String sql =
		    "select " + TABLE_ID + ", " + TABLE_NAME + ", " + UPDATE_DATE_COLUMN_NAME + " from "
		            + TABLE_META_TABLES;
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int tableId = resultCursor.getInt(0);
			String tableName = resultCursor.getString(1);
			String updateDateColumnName = null;
			if (!resultCursor.isNull(2)) updateDateColumnName = resultCursor.getString(2);

			result.add(new MetaTable(tableId, tableName, updateDateColumnName));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		loadTableColumns(result);

		return result;
	}

	private void loadTableColumns(List<MetaTable> metaTables)
	{
		List<MetaColumn> metaColumns = new ArrayList<MetaColumn>();
		String sql =
		    "select " + COLUMN_ID + ", " + TABLE_ID + ", " + COLUMN_NAME + ", " + COLUMN_ORDER
		            + ", " + COLUMN_DATA_TYPE + ", " + IS_PRIMARY_KEY + " from "
		            + TABLE_META_COLUMNS;
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int columnId = resultCursor.getInt(0);
			int tableId = resultCursor.getInt(1);
			String columnName = resultCursor.getString(2);
			int columnOrder = resultCursor.getInt(3);
			String columnDataType = resultCursor.getString(4);
			int isPrimaryKey = resultCursor.getInt(5);

			metaColumns.add(new MetaColumn(columnId, tableId, columnName, columnOrder, getDataType(columnDataType), isPrimaryKey == 1));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		putColumnsToTables(metaTables, metaColumns);
	}

	private DataType<? extends Object> getDataType(String columnDataType)
	{
		if ("SHORT_DATE".equalsIgnoreCase(columnDataType)) return DataTypes.SHORT_DATE_DATA_TYPE;
		if ("LONG_DATE".equalsIgnoreCase(columnDataType)) return DataTypes.LONG_DATE_DATA_TYPE;
		if ("INTEGER".equalsIgnoreCase(columnDataType)) return DataTypes.INTEGER_DATA_TYPE;
		return DataTypes.TEXT_DATA_TYPE;
	}

	private void putColumnsToTables(List<MetaTable> metaTables, List<MetaColumn> metaColumns)
	{
		Map<Integer, MetaTable> tablesMap = createTablesMap(metaTables);
		for (MetaColumn metaColumn : metaColumns)
		{
			MetaTable metaTable = tablesMap.get(metaColumn.getTableId());
			metaTable.addColumn(metaColumn);
			metaColumn.setTable(metaTable);
		}
	}

	private Map<Integer, MetaTable> createTablesMap(List<MetaTable> metaTables)
	{
		Map<Integer, MetaTable> result = new HashMap<Integer, MetaTable>();
		for (MetaTable metaTable : metaTables)
		{
			result.put(metaTable.getTableId(), metaTable);
		}
		return result;
	}
}
