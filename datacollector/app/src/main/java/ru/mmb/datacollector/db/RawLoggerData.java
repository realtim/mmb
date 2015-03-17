package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.util.DateFormat;

public class RawLoggerData {
    private static final String TABLE_RAW_LOGGER_DATA = "RawLoggerData";

    private static final String USER_ID = "user_id";
    private static final String DEVICE_ID = "device_id";
    private static final String LOGGER_ID = "logger_id";
    private static final String SCANPOINT_ID = "scanpoint_id";
    private static final String TEAM_ID = "team_id";
    private static final String RAWLOGGERDATA_DATE = "rawloggerdata_date";

    private final SQLiteDatabase db;

    public RawLoggerData(SQLiteDatabase db) {
        this.db = db;
    }

    public ru.mmb.datacollector.model.RawLoggerData getExistingRecord(int loggerId, int scanpointId, int teamId) {
        ru.mmb.datacollector.model.RawLoggerData result = null;
        String sql =
                "select t." + RAWLOGGERDATA_DATE + " from " + TABLE_RAW_LOGGER_DATA + " as t " +
                " where t." + LOGGER_ID + " = " + loggerId + " and t." + SCANPOINT_ID + " = " +
                scanpointId + " and t." + TEAM_ID + " = " + teamId;
        Cursor resultCursor = db.rawQuery(sql, null);

        if (!resultCursor.isAfterLast()) {
            String rawLoggerDataDate = resultCursor.getString(0);
            result = new ru.mmb.datacollector.model.RawLoggerData(loggerId, scanpointId, teamId, DateFormat.parse(rawLoggerDataDate));
        }
        resultCursor.close();

        return result;
    }

    public void insertNewRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        String sql =
                "insert into " + TABLE_RAW_LOGGER_DATA + "(" + USER_ID + ", " + DEVICE_ID + ", " +
                LOGGER_ID + ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " + RAWLOGGERDATA_DATE +
                ") VALUES" + "(" + Settings.getInstance().getUserId() + ", " +
                Settings.getInstance().getDeviceId() + ", " + loggerId + ", " + scanpointId + ", " +
                teamId + ", '" + DateFormat.format(recordDateTime) + "')";
        db.execSQL(sql);
    }

    public void updateExistingRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        String sql =
                "update " + TABLE_RAW_LOGGER_DATA + " set " + RAWLOGGERDATA_DATE + " = '" +
                DateFormat.format(recordDateTime) + "' where " + LOGGER_ID + " = " + loggerId +
                " and " + SCANPOINT_ID + " = " + scanpointId + " and " + TEAM_ID + " = " + teamId;
        db.execSQL(sql);
    }
}
