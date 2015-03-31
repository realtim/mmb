package ru.mmb.datacollector.transport.model;

import android.database.Cursor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.ExportMode;

public class MetaTable {
    private final int tableId;
    private final String tableName;
    private final String updateDateColumnName;

    private final Map<String, MetaColumn> columnsByName = new HashMap<String, MetaColumn>();
    private final Map<Integer, MetaColumn> columnsByOrder = new TreeMap<Integer, MetaColumn>();

    private String exportWhereAppendix = "";
    private String lastExportDate = "";

    public MetaTable(int tableId, String tableName, String updateDateColumnName) {
        this.tableId = tableId;
        this.tableName = tableName;
        this.updateDateColumnName = updateDateColumnName;
    }

    public int getTableId() {
        return tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getUpdateDateColumnName() {
        return updateDateColumnName;
    }

    public void addColumn(MetaColumn metaColumn) {
        columnsByName.put(metaColumn.getColumnName(), metaColumn);
        columnsByOrder.put(new Integer(metaColumn.getColumnOrder()), metaColumn);
    }

    public MetaColumn getColumnByName(String columnName) {
        return columnsByName.get(columnName);
    }

    public MetaColumn getColumnByOrder(Integer columnOrder) {
        return columnsByOrder.get(columnOrder);
    }

    public List<String> getColumnNames() {
        List<String> result = new ArrayList<String>();
        for (Integer columnOrder : columnsByOrder.keySet()) {
            result.add(columnsByOrder.get(columnOrder).getColumnName());
        }
        return result;
    }

    public Date getUpdateDate(JSONObject tableRow) throws JSONException {
        MetaColumn updateDateColumn = getUpdateDateColumn();
        return (Date) updateDateColumn.getValue(tableRow);
    }

    public String generateUpdateDateSelectSQL(JSONObject tableRow) throws JSONException {
        return "select " + updateDateColumnName + " from " + tableName + " where "
               + generatePKCondition(tableRow);
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

    public MetaColumn getUpdateDateColumn() {
        if (updateDateColumnName == null) return null;
        return getColumnByName(updateDateColumnName);
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

    public void setLastExportDate(String lastExportDate) {
        this.lastExportDate = lastExportDate;
    }

    public String generateCheckNewRecordsSQL(ExportMode exportMode) {
        String selectSql = "select count(*) from " + getTableName();

        String whereClause = generateExportWhereClause(exportMode);
        if (whereClause.length() > 0) {
            selectSql += " where " + whereClause;
        }

        return selectSql;
    }

    public String generateSelectNewRecordsSQL(ExportMode exportMode) {
        String selectSql = "select " + generateColumnNamesClause() + " from " + getTableName();

        String whereClause = generateExportWhereClause(exportMode);
        if (whereClause.length() > 0) {
            selectSql += " where " + whereClause;
        }

        return selectSql;
    }

    private String generateExportWhereClause(ExportMode exportMode) {
        String result = "";
        if (getColumnByName("device_id") != null) {
            result = "device_id = " + Settings.getInstance().getDeviceId();
        }
        if (needSelectByDate(exportMode)) {
            if (result.length() > 0) result += " and ";
            result += getUpdateDateCondition();
        }
        if (exportWhereAppendix.length() > 0) {
            if (result.length() > 0) result += " and ";
            result += exportWhereAppendix;
        }
        Log.d("meta table", "export where clause: " + result);
        return result;
    }

    private boolean needSelectByDate(ExportMode exportMode) {
        if (getUpdateDateColumn() == null) return false;
        if (exportMode == ExportMode.FULL) return false;
        if ("".equals(lastExportDate)) return false;
        return true;
    }

    private String getUpdateDateCondition() {
        String result = getUpdateDateColumnName() + " >= " + "'" + lastExportDate + "'";
        Log.d("meta table", "update date condition: " + result);
        return result;
    }

    public String generateExportRowString(Cursor cursor) {
        StringBuilder result = new StringBuilder();
        int columnIndex = 0;
        for (MetaColumn metaColumn : columnsByOrder.values()) {
            if (columnIndex > 0) result.append(";");
            result.append(metaColumn.decorateForExportToString(cursor));
            columnIndex++;
        }
        return result.toString();
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
        return !("TeamLevelPoints".equals(tableName) || "TeamLevelDismiss".equals(tableName) ||
                 tableName.startsWith("Raw"));
    }
}
