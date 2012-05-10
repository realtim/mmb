package ru.mmb.terminal.transport.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportMode;
import android.database.Cursor;

public class MetaTable
{
	private final int tableId;
	private final String tableName;
	private final String updateDateColumnName;

	private final Map<String, MetaColumn> columnsByName = new HashMap<String, MetaColumn>();
	private final Map<Integer, MetaColumn> columnsByOrder = new TreeMap<Integer, MetaColumn>();

	public MetaTable(int tableId, String tableName, String updateDateColumnName)
	{
		this.tableId = tableId;
		this.tableName = tableName;
		this.updateDateColumnName = updateDateColumnName;
	}

	public int getTableId()
	{
		return tableId;
	}

	public String getTableName()
	{
		return tableName;
	}

	public String getUpdateDateColumnName()
	{
		return updateDateColumnName;
	}

	public void addColumn(MetaColumn metaColumn)
	{
		columnsByName.put(metaColumn.getColumnName(), metaColumn);
		columnsByOrder.put(new Integer(metaColumn.getColumnOrder()), metaColumn);
	}

	public MetaColumn getColumnByName(String columnName)
	{
		return columnsByName.get(columnName);
	}

	public MetaColumn getColumnByOrder(Integer columnOrder)
	{
		return columnsByOrder.get(columnOrder);
	}

	public Date getUpdateDate(JSONObject tableRow) throws JSONException
	{
		MetaColumn updateDateColumn = getUpdateDateColumn();
		return (Date) updateDateColumn.getValue(tableRow);
	}

	public String generateUpdateDateSelectSQL(JSONObject tableRow) throws JSONException
	{
		return "select " + updateDateColumnName + " from " + tableName + " where "
		        + generatePKCondition(tableRow);
	}

	private String generatePKCondition(JSONObject tableRow) throws JSONException
	{
		StringBuilder sb = new StringBuilder();
		int keyColumnIndex = 0;
		for (MetaColumn column : columnsByOrder.values())
		{
			if (column.isPrimaryKey())
			{
				if (keyColumnIndex != 0) sb.append(" and ");
				sb.append(column.getColumnName()).append(" = ").append(column.decorateForDBQuery(tableRow));
				keyColumnIndex++;
			}
		}
		return sb.toString();
	}

	public MetaColumn getUpdateDateColumn()
	{
		if (updateDateColumnName == null) return null;
		return getColumnByName(updateDateColumnName);
	}

	public String generateCheckExistsSQL(JSONObject tableRow) throws JSONException
	{
		return "select count(*) from " + tableName + " where " + generatePKCondition(tableRow);
	}

	public String generateUpdateSQL(JSONObject tableRow) throws JSONException
	{
		return "update " + tableName + " set " + generateColumnsUpdateClause(tableRow) + " where "
		        + generatePKCondition(tableRow);
	}

	private String generateColumnsUpdateClause(JSONObject tableRow) throws JSONException
	{
		StringBuilder sb = new StringBuilder();
		int columnIndex = 0;
		for (MetaColumn column : columnsByOrder.values())
		{
			if (!column.isPrimaryKey())
			{
				if (columnIndex != 0) sb.append(", ");
				sb.append(column.getColumnName()).append(" = ").append(column.decorateForDBQuery(tableRow));
				columnIndex++;
			}
		}
		return sb.toString();
	}

	public String generateInsertSQL(JSONObject tableRow) throws JSONException
	{
		return "insert into " + tableName + "(" + generateColumnNamesClause() + ") values ("
		        + generateColumnValuesClause(tableRow) + ")";
	}

	private String generateColumnNamesClause()
	{
		StringBuilder sb = new StringBuilder();
		int columnIndex = 0;
		for (MetaColumn column : columnsByOrder.values())
		{
			if (columnIndex != 0) sb.append(", ");
			sb.append(column.getColumnName());
			columnIndex++;
		}
		return sb.toString();
	}

	private String generateColumnValuesClause(JSONObject tableRow) throws JSONException
	{
		StringBuilder sb = new StringBuilder();
		int columnIndex = 0;
		for (MetaColumn column : columnsByOrder.values())
		{
			if (columnIndex != 0) sb.append(", ");
			sb.append(column.decorateForDBQuery(tableRow));
			columnIndex++;
		}
		return sb.toString();
	}

	public String generateCheckNewRecordsSQL()
	{
		return "select count(*) from " + getTableName() + " where " + getUpdateDateCondition();
	}

	private String getUpdateDateCondition()
	{
		return getUpdateDateColumnName() + " >= " + "'"
		        + Settings.getInstance().getLastExportDate() + "'";
	}

	public String generateSelectNewRecordsSQL(ExportMode exportMode)
	{
		String selectSql = "select " + generateColumnNamesClause() + " from " + getTableName();

		String whereCondition = "";
		if (getColumnByName("device_id") != null)
		{
			whereCondition = "device_id = " + Settings.getInstance().getDeviceId();
		}
		if (needSelectByDate(exportMode))
		{
			if (whereCondition.length() > 0)
			{
				whereCondition += " and ";
			}
			whereCondition += getUpdateDateCondition();
		}
		if (whereCondition.length() > 0)
		{
			selectSql += " where " + whereCondition;
		}

		return selectSql;
	}

	private boolean needSelectByDate(ExportMode exportMode)
	{
		if (getUpdateDateColumn() == null) return false;
		if (exportMode == ExportMode.FULL) return false;
		if ("".equals(Settings.getInstance().getLastExportDate())) return false;
		return true;
	}

	public String generateExportRowString(Cursor cursor)
	{
		StringBuilder result = new StringBuilder();
		int columnIndex = 0;
		for (MetaColumn metaColumn : columnsByOrder.values())
		{
			if (columnIndex > 0) result.append(";");
			result.append(metaColumn.decorateForExport(cursor));
			columnIndex++;
		}
		return result.toString();
	}
}
