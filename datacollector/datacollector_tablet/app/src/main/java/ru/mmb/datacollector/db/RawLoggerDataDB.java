package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
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
    private static final String SCANNED_DATE = "scanned_date";
    private static final String CHANGED_MANUAL = "changed_manual";

    private final SQLiteDatabase db;

    public RawLoggerDataDB(SQLiteDatabase db) {
        this.db = db;
    }

    public RawLoggerData getExistingRecord(int loggerId, int scanpointId, int teamId) {
        RawLoggerData result = null;
        String sql =
                "select t." + RAWLOGGERDATA_DATE + ", t." + SCANNED_DATE + ", t." + CHANGED_MANUAL +
                        " from " + TABLE_RAW_LOGGER_DATA + " as t " + " where t." + LOGGER_ID +
                        " = " + loggerId + " and t." + SCANPOINT_ID + " = " + scanpointId + " and t." +
                        TEAM_ID + " = " + teamId;
        Cursor resultCursor = db.rawQuery(sql, null);

        if (resultCursor.moveToFirst() == false) {
            resultCursor.close();
            return null;
        }

        String recordDateTime = resultCursor.getString(0);
        String scannedDateTime = resultCursor.getString(1);
        int changedManual = resultCursor.getInt(2);
        result = new RawLoggerData(loggerId, scanpointId, teamId, DateFormat.parse(recordDateTime), DateFormat.parse(scannedDateTime), changedManual);

        result.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
        result.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanpointId));
        resultCursor.close();

        return result;
    }

    public RawLoggerData getExistingRecord(ScanPoint scanPoint, Team team) {
        RawLoggerData result = null;
        String sql =
                "select t." + RAWLOGGERDATA_DATE + ", t." + LOGGER_ID + ", t." + SCANNED_DATE +
                        ", t." + CHANGED_MANUAL + " from " + TABLE_RAW_LOGGER_DATA +
                        " as t " + " where t." + SCANPOINT_ID + " = " + scanPoint.getScanPointId() +
                        " and t." + TEAM_ID + " = " + team.getTeamId() + " order by " + CHANGED_MANUAL +
                        " desc, " + RAWLOGGERDATA_DATE + " desc";
        Cursor resultCursor = db.rawQuery(sql, null);

        if (resultCursor.moveToFirst() == false) {
            resultCursor.close();
            return null;
        }

        String recordDateTime = resultCursor.getString(0);
        int loggerId = resultCursor.getInt(1);
        String scannedDateTime = resultCursor.getString(2);
        int changedManual = resultCursor.getInt(3);
        result = new RawLoggerData(loggerId, scanPoint.getScanPointId(), team.getTeamId(), DateFormat.parse(recordDateTime), DateFormat.parse(scannedDateTime), changedManual);

        result.setTeam(team);
        result.setScanPoint(scanPoint);
        resultCursor.close();

        return result;
    }

    public String getInsertNewLoggerRecordSql(int loggerId, int scanpointId, int teamId, Date recordDateTime, Date scannedDateTime, int changedManual) {
        String sql =
                "insert into " + TABLE_RAW_LOGGER_DATA + "(" + USER_ID + ", " + DEVICE_ID + ", " +
                        LOGGER_ID + ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " + RAWLOGGERDATA_DATE +
                        ", " + SCANNED_DATE + ", " + CHANGED_MANUAL +
                        ") VALUES" + "(" + Settings.getInstance().getUserId() + ", " +
                        Settings.getInstance().getDeviceId() + ", " + loggerId + ", " + scanpointId + ", " +
                        teamId + ", '" + DateFormat.format(recordDateTime) + "', '" +
                        DateFormat.format(scannedDateTime) + "', " + changedManual + ")";
        return sql;
    }

    public String getUpdateExistingLoggerRecordSql(int loggerId, int scanpointId, int teamId, Date recordDateTime, Date scannedDateTime, int changedManual) {
        String sql =
                "update " + TABLE_RAW_LOGGER_DATA + " set " + RAWLOGGERDATA_DATE + " = '" +
                        DateFormat.format(recordDateTime) + "', " + SCANNED_DATE + " = '" +
                        DateFormat.format(scannedDateTime) + "', " + CHANGED_MANUAL + " = " + changedManual
                        + " where " + LOGGER_ID + " = " + loggerId +
                        " and " + SCANPOINT_ID + " = " + scanpointId + " and " + TEAM_ID + " = " + teamId;
        return sql;
    }

    public void saveRawLoggerDataManual(ScanPoint scanPoint, Team team, Date scannedDateTime, Date recordDateTime) {
        db.beginTransaction();
        try {
            RawLoggerData prevRecord = getExistingRecord(scanPoint, team);
            if (prevRecord != null) {
                updateExistingLoggerRecordManual(prevRecord, scannedDateTime, recordDateTime);
            } else {
                insertNewLoggerRecordManual(scanPoint, team, scannedDateTime, recordDateTime);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void updateExistingLoggerRecordManual(RawLoggerData prevRecord, Date scannedDateTime, Date recordDateTime) {
        String sql =
                "update " + TABLE_RAW_LOGGER_DATA + " set " + RAWLOGGERDATA_DATE + " = '" +
                        DateFormat.format(recordDateTime) + "', " + SCANNED_DATE + " = '" +
                        DateFormat.format(scannedDateTime) + "', " + CHANGED_MANUAL + " = 1" +
                        " where " + LOGGER_ID + " = " + prevRecord.getLoggerId() + " and " +
                        SCANPOINT_ID + " = " + prevRecord.getScanPointId() + " and " +
                        TEAM_ID + " = " + prevRecord.getTeamId();
        db.execSQL(sql);
    }

    private void insertNewLoggerRecordManual(ScanPoint scanPoint, Team team, Date scannedDateTime, Date recordDateTime) {
        String sql =
                "insert into " + TABLE_RAW_LOGGER_DATA + "(" + USER_ID + ", " + DEVICE_ID + ", " +
                        LOGGER_ID + ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " + RAWLOGGERDATA_DATE +
                        ", " + SCANNED_DATE + ", " + CHANGED_MANUAL +
                        ") VALUES" + "(" + Settings.getInstance().getUserId() + ", " +
                        Settings.getInstance().getDeviceId() + ", -1, " + scanPoint.getScanPointId() + ", " +
                        team.getTeamId() + ", '" + DateFormat.format(recordDateTime) + "', '" +
                        DateFormat.format(scannedDateTime) + "', 1)";
        db.execSQL(sql);
    }

    public List<RawLoggerData> loadRawLoggerData(ScanPoint scanPoint) {

        /*select t.scanpoint_id, t.team_id, t.scanned_date, t.rawloggerdata_date
        from RawLoggerData t
        where t.rawloggerdata_date in
        (select max(t1.rawloggerdata_date) from RawLoggerData t1
        where t1.scanpoint_id = t.scanpoint_id
        and t1.team_id = t.team_id)*/

//        String sql =
//                "select t." + RAWLOGGERDATA_DATE + ", t." + LOGGER_ID + ", t." + SCANNED_DATE +
//                        ", t." + CHANGED_MANUAL + " from " + TABLE_RAW_LOGGER_DATA +
//                        " as t " + " where t." + SCANPOINT_ID + " = " + scanPoint.getScanPointId() +
//                        " and t." + TEAM_ID + " = " + team.getTeamId() + " order by " + CHANGED_MANUAL +
//                        " desc, " + RAWLOGGERDATA_DATE + " desc";
//        Cursor resultCursor = db.rawQuery(sql, null);

        List<RawLoggerData> result = new ArrayList<RawLoggerData>();
        String whereCondition = SCANPOINT_ID + " = " + scanPoint.getScanPointId();
        Cursor resultCursor =
                db.query(TABLE_RAW_LOGGER_DATA, new String[]{LOGGER_ID, TEAM_ID, SCANNED_DATE}, whereCondition, null, null, null, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            int loggerId = resultCursor.getInt(0);
            int teamId = resultCursor.getInt(1);
            Date scannedDate = DateFormat.parse(resultCursor.getString(2));

            RawLoggerData rawLoggerData = new RawLoggerData(loggerId, scanPoint.getScanPointId(), teamId, scannedDate, scannedDate, 0);
            // init refrence fields
            rawLoggerData.setScanPoint(scanPoint);
            rawLoggerData.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));

            result.add(rawLoggerData);
            resultCursor.moveToNext();
        }
        resultCursor.close();

        return result;
    }
}
