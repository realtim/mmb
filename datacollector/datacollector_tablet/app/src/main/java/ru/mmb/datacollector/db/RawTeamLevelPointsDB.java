package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawTeamLevelPointsDB {
    private static final String TABLE_RAW_TEAM_LEVEL_POINTS = "RawTeamLevelPoints";

    private static final String USER_ID = "user_id";
    private static final String DEVICE_ID = "device_id";
    private static final String SCANPOINT_ID = "scanpoint_id";
    private static final String TEAM_ID = "team_id";
    private static final String RAWTEAMLEVELPOINTS_POINTS = "rawteamlevelpoints_points";
    private static final String RAWTEAMLEVELPOINTS_DATE = "rawteamlevelpoints_date";

    private final SQLiteDatabase db;

    public RawTeamLevelPointsDB(SQLiteDatabase db) {
        this.db = db;
    }

    public RawTeamLevelPointsRecord getExistingTeamResultRecord(ScanPoint scanPoint, Team team) {
        RawTeamLevelPointsRecord result = null;
        String sql =
                "select " + RAWTEAMLEVELPOINTS_DATE + ", " + RAWTEAMLEVELPOINTS_POINTS + " from " +
                TABLE_RAW_TEAM_LEVEL_POINTS + " where " + SCANPOINT_ID + " = " +
                scanPoint.getScanPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
        Cursor resultCursor = db.rawQuery(sql, null);

        if (resultCursor.moveToFirst() == false) {
            resultCursor.close();
            return null;
        }

        List<RawTeamLevelPointsRecord> records = new ArrayList<RawTeamLevelPointsRecord>();
        while (!resultCursor.isAfterLast()) {
            String recordDateTime = resultCursor.getString(0);
            String takenCheckpoints = replaceNullWithEmptyString(resultCursor.getString(1));
            records.add(new RawTeamLevelPointsRecord(recordDateTime, takenCheckpoints, scanPoint, team));
            resultCursor.moveToNext();
        }
        resultCursor.close();

        Collections.sort(records);
        if (records.size() > 0) result = records.get(records.size() - 1);

        return result;
    }

    public List<RawTeamLevelPoints> loadRawTeamLevelPoints(ScanPoint scanPoint) {
        List<RawTeamLevelPoints> result = new ArrayList<RawTeamLevelPoints>();
        String sql =
                "select " + USER_ID + ", " + DEVICE_ID + ", " + TEAM_ID
                + ", " + RAWTEAMLEVELPOINTS_DATE + ", " + RAWTEAMLEVELPOINTS_POINTS + " from "
                + TABLE_RAW_TEAM_LEVEL_POINTS + " where " + SCANPOINT_ID + " = "
                + scanPoint.getScanPointId();
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            Integer userId = resultCursor.getInt(0);
            Integer deviceId = resultCursor.getInt(1);
            Integer teamId = resultCursor.getInt(2);
            Date recordDateTime = DateFormat.parse(resultCursor.getString(3));
            String takenCheckpointNames = replaceNullWithEmptyString(resultCursor.getString(5));

            RawTeamLevelPoints rawTeamLevelPoints =
                    new RawTeamLevelPoints(teamId, userId, deviceId, scanPoint.getScanPointId(), takenCheckpointNames, recordDateTime);
            // init reference fields
            rawTeamLevelPoints.setScanPoint(scanPoint);
            rawTeamLevelPoints.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
            rawTeamLevelPoints.initTakenCheckpoints();

            result.add(rawTeamLevelPoints);
            resultCursor.moveToNext();
        }
        resultCursor.close();

        return result;
    }

    private String replaceNullWithEmptyString(String takenCheckpoints) {
        if ("NULL".equals(takenCheckpoints)) {
            return "";
        } else {
            return takenCheckpoints;
        }
    }

    public void appendScanPointTeams(ScanPoint scanPoint, Set<Integer> teams) {
        String sql =
                "select distinct " + TEAM_ID + " from " + TABLE_RAW_TEAM_LEVEL_POINTS + " where "
                + SCANPOINT_ID + " = " + scanPoint.getScanPointId();
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            Integer teamId = resultCursor.getInt(0);
            teams.add(teamId);
            resultCursor.moveToNext();
        }
        resultCursor.close();
    }

    public void saveRawTeamLevelPoints(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime) {
        db.beginTransaction();
        try {
            if (isThisUserRecordExists(scanPoint, team)) {
                updateExistingRecord(scanPoint, team, takenCheckpoints, recordDateTime);
            } else {
                insertNewRecord(scanPoint, team, takenCheckpoints, recordDateTime);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private boolean isThisUserRecordExists(ScanPoint scanPoint, Team team) {
        String selectSql =
                "select count(*) from " + TABLE_RAW_TEAM_LEVEL_POINTS + " where " + USER_ID + " = "
                + Settings.getInstance().getUserId() + " and " + SCANPOINT_ID + " = "
                + scanPoint.getScanPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
        Cursor resultCursor = db.rawQuery(selectSql, null);
        resultCursor.moveToFirst();
        int recordCount = resultCursor.getInt(0);
        resultCursor.close();
        return recordCount > 0;
    }

    private void updateExistingRecord(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime) {
        String updateSql =
                "update " + TABLE_RAW_TEAM_LEVEL_POINTS + " set " + DEVICE_ID + " = " +
                Settings.getInstance().getDeviceId() + ", " + RAWTEAMLEVELPOINTS_DATE + " = '" +
                DateFormat.format(recordDateTime) + "', " + RAWTEAMLEVELPOINTS_POINTS + " = '" +
                takenCheckpoints + "' where " + USER_ID + " = " +
                Settings.getInstance().getUserId() + " and " + SCANPOINT_ID + " = " +
                scanPoint.getScanPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
        db.execSQL(updateSql);
    }

    private void insertNewRecord(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime) {
        String insertSql =
                "insert into " + TABLE_RAW_TEAM_LEVEL_POINTS + " (" + USER_ID + ", " + DEVICE_ID +
                ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " + RAWTEAMLEVELPOINTS_DATE + ", " +
                RAWTEAMLEVELPOINTS_POINTS + ") values (" + Settings.getInstance().getUserId() +
                ", " + Settings.getInstance().getDeviceId() + ", " + scanPoint.getScanPointId() +
                ", " + team.getTeamId() + ", '" + DateFormat.format(recordDateTime) + "', '" +
                takenCheckpoints + "')";
        db.execSQL(insertSql);
    }
}
