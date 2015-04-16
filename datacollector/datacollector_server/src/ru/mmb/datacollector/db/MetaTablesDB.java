package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.meta.MetaColumn;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.meta.datatype.DataType;
import ru.mmb.datacollector.model.meta.datatype.DataTypes;

public class MetaTablesDB {
	private static final Logger logger = LogManager.getLogger(MetaTablesDB.class);

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

	public static synchronized List<MetaTable> loadMetaTables() {
		List<MetaTable> result = new ArrayList<MetaTable>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + TABLE_ID + "`, `" + TABLE_NAME + "`, `" + UPDATE_DATE_COLUMN_NAME
						+ "` from `" + TABLE_META_TABLES + "`";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int tableId = rs.getInt(1);
					String tableName = rs.getString(2);
					String updateDateColumnName = rs.getString(3);
					if (rs.wasNull()) {
						updateDateColumnName = null;
					}
					result.add(new MetaTable(tableId, tableName, updateDateColumnName));
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		loadTableColumns(result);

		return result;
	}

	private static void loadTableColumns(List<MetaTable> metaTables) {
		List<MetaColumn> metaColumns = new ArrayList<MetaColumn>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + COLUMN_ID + "`, `" + TABLE_ID + "`, `" + COLUMN_NAME + "`, `" + COLUMN_ORDER
						+ "`, `" + COLUMN_DATA_TYPE + "`, `" + IS_PRIMARY_KEY + "` from `" + TABLE_META_COLUMNS + "`";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int columnId = rs.getInt(1);
					int tableId = rs.getInt(2);
					String columnName = rs.getString(3);
					int columnOrder = rs.getInt(4);
					String columnDataType = rs.getString(5);
					int isPrimaryKey = rs.getInt(6);
					metaColumns.add(new MetaColumn(columnId, tableId, columnName, columnOrder,
							getDataType(columnDataType), isPrimaryKey == 1));
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		putColumnsToTables(metaTables, metaColumns);
	}

	private static DataType<? extends Object> getDataType(String columnDataType) {
		if ("SHORT_DATE".equalsIgnoreCase(columnDataType))
			return DataTypes.SHORT_DATE_DATA_TYPE;
		if ("LONG_DATE".equalsIgnoreCase(columnDataType))
			return DataTypes.LONG_DATE_DATA_TYPE;
		if ("INTEGER".equalsIgnoreCase(columnDataType))
			return DataTypes.INTEGER_DATA_TYPE;
		return DataTypes.TEXT_DATA_TYPE;
	}

	private static void putColumnsToTables(List<MetaTable> metaTables, List<MetaColumn> metaColumns) {
		Map<Integer, MetaTable> tablesMap = createTablesMap(metaTables);
		for (MetaColumn metaColumn : metaColumns) {
			MetaTable metaTable = tablesMap.get(metaColumn.getTableId());
			metaTable.addColumn(metaColumn);
			metaColumn.setTable(metaTable);
		}
	}

	private static Map<Integer, MetaTable> createTablesMap(List<MetaTable> metaTables) {
		Map<Integer, MetaTable> result = new HashMap<Integer, MetaTable>();
		for (MetaTable metaTable : metaTables) {
			result.put(metaTable.getTableId(), metaTable);
		}
		return result;
	}
}
