package ru.mmb.datacollector.transport.exporter.method;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataExtractorFromDBToJson extends DataExtractorFromDB {
	private JSONArray records;

	public DataExtractorFromDBToJson() throws SQLException, Exception {
		super();
	}

	public void setTargetRecords(JSONArray records) {
		this.records = records;
	}

	@Override
	protected void exportRow(ResultSet rs) throws Exception {
		JSONObject rowJSON = getCurrentTable().generateExportRowJSON(rs);
		records.put(rowJSON);
	}
}
