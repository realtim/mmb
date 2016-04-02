package ru.mmb.datacollector.model.meta;

import android.database.Cursor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.datacollector.model.registry.Settings;

public class MetaTable {
    private final int tableId;
    private final String tableName;

    private final Map<String, MetaColumn> columnsByName = new HashMap<String, MetaColumn>();
    private final Map<Integer, MetaColumn> columnsByOrder = new TreeMap<Integer, MetaColumn>();

    private String exportWhereAppendix = "";

    public MetaTable(int tableId, String tableName) {
        this.tableId = tableId;
        this.tableName = tableName;
    }

    public int getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void addColumn(MetaColumn metaColumn) {
        columnsByName.put(metaColumn.getColumnName(), metaColumn);
        columnsByOrder.put(metaColumn.getColumnOrder(), metaColumn);
    }

    public MetaColumn getColumnByName(String columnName) {
        return columnsByName.get(columnName);
    }

    protected String generatePKCondition(JSONObject tableRow) throws JSONException {
        StringBuilder sb = new StringBuilder();
        int keyColumnIndex = 0;
        for (MetaColumn column : columnsByOrder.values()) {
            if (column.isPrimaryKey()) {
                if (keyColumnIndex != 0) sb.append(" and ");
                sb.append(column.getColumnName()).append(" = ").append(column.decorateForDBQuery(tableRow));
                keyColumnIndex++;
            }
        }
        return sb.toString();
    }

    public String generateCheckExistsSQL(JSONObject tableRow) throws JSONException {
        return "select count(*) from " + tableName + " where " + generatePKCondition(tableRow);
    }

    public String generateUpdateSQL(JSONObject tableRow) throws JSONException {
        return "update " + tableName + " set " + generateColumnsUpdateClause(tableRow) + " where "
                + generatePKCondition(tableRow);
    }

    private String generateColumnsUpdateClause(JSONObject tableRow) throws JSONException {
        StringBuilder sb = new StringBuilder();
        int columnIndex = 0;
        for (MetaColumn column : columnsByOrder.values()) {
            if (!column.isPrimaryKey()) {
                if (columnIndex != 0) sb.append(", ");
                sb.append(column.getColumnName()).append(" = ").append(column.decorateForDBQuery(tableRow));
                columnIndex++;
            }
        }
        return sb.toString();
    }

    public String generateInsertSQL(JSONObject tableRow) throws JSONException {
        return "insert into " + tableName + "(" + generateColumnNamesClause() + ") values ("
                + generateColumnValuesClause(tableRow) + ")";
    }

    private String generateColumnNamesClause() {
        StringBuilder sb = new StringBuilder();
        int columnIndex = 0;
        for (MetaColumn column : columnsByOrder.values()) {
            if (columnIndex != 0) sb.append(", ");
            sb.append(column.getColumnName());
            columnIndex++;
        }
        return sb.toString();
    }

    private String generateColumnValuesClause(JSONObject tableRow) throws JSONException {
        StringBuilder sb = new StringBuilder();
        int columnIndex = 0;
        for (MetaColumn column : columnsByOrder.values()) {
            if (columnIndex != 0) sb.append(", ");
            sb.append(column.decorateForDBQuery(tableRow));
            columnIndex++;
        }
        return sb.toString();
    }

    public void setExportWhereAppendix(String exportWhereAppendix) {
        this.exportWhereAppendix = exportWhereAppendix;
    }

    public String generateSelectAllRecordsSQL() {
        String selectSql = "select " + generateColumnNamesClause() + " from " + getTableName();

        String whereClause = generateExportWhereClause();
        if (whereClause.length() > 0) {
            selectSql += " where " + whereClause;
        }

        return selectSql;
    }

    private String generateExportWhereClause() {
        String result = "";
        if (getColumnByName("device_id") != null) {
            result = "device_id = " + Settings.getInstance().getDeviceId();
        }
        if (exportWhereAppendix.length() > 0) {
            if (result.length() > 0) result += " and ";
            result += exportWhereAppendix;
        }
        Log.d("meta table", "export where clause: " + result);
        return result;
    }

    public String generateDeleteAllRowsSQL() {
        return "delete from " + getTableName();
    }

    public JSONObject generateExportRowJSON(Cursor cursor) throws JSONException {
        JSONObject result = new JSONObject();
        for (MetaColumn metaColumn : columnsByOrder.values()) {
            result.put(metaColumn.getColumnName(), metaColumn.decorateForExportToJSON(cursor));
        }
        return result;
    }

    public boolean needClearBeforeImport() {
        return !tableName.startsWith("Raw");
    }
}
