package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;

import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawLoggerDataDB {
    private static final String TABLE_RAW_LOGGER_DATA = "RawLoggerData";

    private static final String USER_ID = "user_id";
    private static final String DEVICE_ID = "device_id";
    private static final String LOGGER_ID = "logger_id";
    private static final String SCANPOINT_ID = "scanpoint_id";
    private static final String TEAM_ID = "team_id";
    private static final String RAWLOGGERDATA_DATE = "rawloggerdata_date";

    private final SQLiteDatabase db;

    public RawLoggerDataDB(SQLiteDatabase db) {
        this.db = db;
    }

    public RawLoggerData getExistingRecord(int loggerId, int scanpointId, int teamId) {
        RawLoggerData result = null;
        String sql =
                "select t." + RAWLOGGERDATA_DATE + " from " + TABLE_RAW_LOGGER_DATA + " as t " +
                " where t." + LOGGER_ID + " = " + loggerId + " and t." + SCANPOINT_ID + " = " +
                scanpointId + " and t." + TEAM_ID + " = " + teamId;
        Cursor resultCursor = db.rawQuery(sql, null);

        if (resultCursor.moveToFirst() == false) {
            resultCursor.close();
            return null;
        }

        String recordDateTime = resultCursor.getString(0);
        result = new RawLoggerData(loggerId, scanpointId, teamId, DateFormat.parse(recordDateTime));

        result.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
        result.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanpointId));
        resultCursor.close();

        return result;
    }

    public void insertNewRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        db.beginTransaction();
        try {
            String sql =
                    "insert into " + TABLE_RAW_LOGGER_DATA + "(" + USER_ID + ", " + DEVICE_ID +
                    ", " + LOGGER_ID + ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " +
                    RAWLOGGERDATA_DATE + ") VALUES" + "(" + Settings.getInstance().getUserId() +
                    ", " + Settings.getInstance().getDeviceId() + ", " + loggerId + ", " +
                    scanpointId + ", " + teamId + ", '" + DateFormat.format(recordDateTime) + "')";
            db.execSQL(sql);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateExistingRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        db.beginTransaction();
        try {
            String sql =
                    "update " + TABLE_RAW_LOGGER_DATA + " set " + RAWLOGGERDATA_DATE + " = '" +
                    DateFormat.format(recordDateTime) + "' where " + LOGGER_ID + " = " + loggerId +
                    " and " + SCANPOINT_ID + " = " + scanpointId + " and " + TEAM_ID + " = " +
                    teamId;
            db.execSQL(sql);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
