package ru.mmb.datacollector.transport.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.registry.MetaTablesRegistry;

public class ImportBarCodeMetaTable extends MetaTable
{
	private static final String TABLE_BAR_CODE_SCANS = "BarCodeScans";
	private static final String TABLE_TEAM_LEVEL_POINTS = "TeamLevelPoints";

	// converting date field
	private static final String BARCODESCAN_DATE = "barcodescan_date";
	private static final String TEAMLEVELPOINT_DATE = "teamlevelpoint_date";
	// common fields
	private static final String DEVICE_ID = "device_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMLEVELPOINT_DATETIME = "teamlevelpoint_datetime";
	// fields from TeamLevelPoints
	private static final String USER_ID = "user_id";
	private static final String TEAMLEVELPOINT_POINTS = "teamlevelpoint_points";
	private static final String TEAMLEVELPOINT_COMMENT = "teamlevelpoint_comment";

	private static Map<String, String> columnMapping = new HashMap<String, String>();

	static
	{
		columnMapping.put(BARCODESCAN_DATE, TEAMLEVELPOINT_DATE);
		columnMapping.put(LEVELPOINT_ID, LEVELPOINT_ID);
		columnMapping.put(DEVICE_ID, DEVICE_ID);
		columnMapping.put(TEAM_ID, TEAM_ID);
		columnMapping.put(TEAMLEVELPOINT_DATETIME, TEAMLEVELPOINT_DATETIME);
	}

	private final MetaTable barCodeTable;
	private final MetaTable teamLevelPointsTable;

	public ImportBarCodeMetaTable()
	{
		super(-1, "", "");
		barCodeTable = MetaTablesRegistry.getInstance().getTableByName(TABLE_BAR_CODE_SCANS);
		teamLevelPointsTable =
		    MetaTablesRegistry.getInstance().getTableByName(TABLE_TEAM_LEVEL_POINTS);
	}

	@Override
	public String getTableName()
	{
		return "BarCodeScans";
	}

	@Override
	public MetaColumn getColumnByName(String columnName)
	{
		throw new RuntimeException("ImportBarCodeMetaTable.getColumnByName must not be used.");
		// return barCodeTable.getColumnByName(columnName);
	}

	@Override
	public MetaColumn getColumnByOrder(Integer columnOrder)
	{
		throw new RuntimeException("ImportBarCodeMetaTable.getColumnByOrder must not be used.");
		// return barCodeTable.getColumnByOrder(columnOrder);
	}

	@Override
	public MetaColumn getUpdateDateColumn()
	{
		// Not clear moment, but this method must 
		// return column from TeamLevelPointsTable.
		// It is used during parsing cursor when checking update date in DB record.
		return teamLevelPointsTable.getUpdateDateColumn();
	}

	@Override
	public String generateDeleteAllRowsSQL()
	{
		throw new RuntimeException("ImportBarCodeMetaTable.generateDeleteAllRowsSQL must not be used.");
	}

	@Override
	public Date getUpdateDate(JSONObject tableRow) throws JSONException
	{
		return barCodeTable.getUpdateDate(tableRow);
	}

	@Override
	public String generateUpdateDateSelectSQL(JSONObject tableRow) throws JSONException
	{
		JSONObject convertedTableRow = convert(tableRow);
		return teamLevelPointsTable.generateUpdateDateSelectSQL(convertedTableRow);
	}

	@Override
	public String generateCheckExistsSQL(JSONObject tableRow) throws JSONException
	{
		JSONObject convertedTableRow = convert(tableRow);
		return teamLevelPointsTable.generateCheckExistsSQL(convertedTableRow);
	}

	@Override
	public String generateUpdateSQL(JSONObject tableRow) throws JSONException
	{
		JSONObject convertedTableRow = convert(tableRow);
		return teamLevelPointsTable.generateUpdateSQL(convertedTableRow);
	}

	@Override
	public String generateInsertSQL(JSONObject tableRow) throws JSONException
	{
		JSONObject convertedTableRow = convert(tableRow);
		return teamLevelPointsTable.generateInsertSQL(convertedTableRow);
	}

	@Override
	public boolean needClearBeforeImport()
	{
		return false;
	}

	@Override
	protected String generatePKCondition(JSONObject tableRow) throws JSONException
	{
		JSONObject convertedTableRow = convert(tableRow);
		return teamLevelPointsTable.generatePKCondition(convertedTableRow);
	}

	private JSONObject convert(JSONObject tableRow) throws JSONException
	{
		JSONObject result = new JSONObject();
		appendExistingColumns(result, tableRow);
		appendNotExistingColumns(result);
		return result;
	}

	private void appendExistingColumns(JSONObject targetJSON, JSONObject tableRow)
	        throws JSONException
	{
		for (String columnName : barCodeTable.getColumnNames())
		{
			MetaColumn column = barCodeTable.getColumnByName(columnName);
			MetaColumn targetColumn =
			    teamLevelPointsTable.getColumnByName(columnMapping.get(columnName));
			Object value = column.getValue(tableRow);
			targetColumn.appendToJSON(targetJSON, value);
		}
	}

	private void appendNotExistingColumns(JSONObject targetJSON) throws JSONException
	{
		teamLevelPointsTable.getColumnByName(USER_ID).appendToJSON(targetJSON, new Integer(Settings.getInstance().getUserId()));
		teamLevelPointsTable.getColumnByName(TEAMLEVELPOINT_POINTS).appendToJSON(targetJSON, "NULL");
		teamLevelPointsTable.getColumnByName(TEAMLEVELPOINT_COMMENT).appendToJSON(targetJSON, null);
	}
}
