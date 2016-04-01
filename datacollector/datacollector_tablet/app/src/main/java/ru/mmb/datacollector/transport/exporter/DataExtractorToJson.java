package ru.mmb.datacollector.transport.exporter;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataExtractorToJson extends DataExtractor {
    private JSONArray records;

    public void setTargetRecords(JSONArray records) {
        this.records = records;
    }

    @Override
    protected void exportRow(Cursor cursor) throws Exception {
        JSONObject rowJSON = getCurrentTable().generateExportRowJSON(cursor);
        records.put(rowJSON);
    }
}
