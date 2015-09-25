package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.model.registry.UsersRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawTeamLevelDismissDB {
    private static final String TABLE_RAW_TEAM_LEVEL_DISMISS = "RawTeamLevelDismiss";
    private static final String TABLE_SCANPOINTS = "ScanPoints";

    private static final String USER_ID = "user_id";
    private static final String DEVICE_ID = "device_id";
    private static final String SCANPOINT_ID = "scanpoint_id";
    private static final String TEAM_ID = "team_id";
    private static final String TEAMUSER_ID = "teamuser_id";
    private static final String DISMISS_DATE = "rawteamleveldismiss_date";
    private static final String SCANPOINT_ORDER = "scanpoint_order";

    private static final String TEMPLATE_TEAMUSER_ID = "%teamuser_id%";

    private final SQLiteDatabase db;

    public RawTeamLevelDismissDB(SQLiteDatabase db) {
        this.db = db;
    }

    public List<Participant> getDismissedMembers(ScanPoint scanPoint, Team team) {
        List<Participant> result = new ArrayList<Participant>();
        String sql =
                "select distinct d." + TEAMUSER_ID + ", sp." + SCANPOINT_ORDER + " from " +
                        TABLE_RAW_TEAM_LEVEL_DISMISS + " as d join " + TABLE_SCANPOINTS + " as sp on (d." +
                        SCANPOINT_ID + " = sp." + SCANPOINT_ID + ") where d." + TEAM_ID + " = " +
                        team.getTeamId() + " and sp." + SCANPOINT_ORDER + " <= " +
                        scanPoint.getScanPointOrder();
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            int participantId = resultCursor.getInt(0);
            int dbScanPointOrder = resultCursor.getInt(1);
            if (isDBScanPointEarlier(scanPoint, dbScanPointOrder)) {
                Participant member = team.getMember(participantId);
                if (!result.contains(member)) {
                    result.add(member);
                }
            }
            resultCursor.moveToNext();
        }
        resultCursor.close();

        return result;
    }

    private boolean isDBScanPointEarlier(ScanPoint scanPoint, int dbScanPointOrder) {
        return scanPoint.getScanPointOrder() >= dbScanPointOrder;
    }

    public void saveDismissedMembers(ScanPoint scanPoint, Team team,
                                     List<Participant> dismissedMembers, Date recordDateTime) {
        String selectSql =
                "select count(*) from " + TABLE_RAW_TEAM_LEVEL_DISMISS + " where " + USER_ID +
                        " = " + Settings.getInstance().getUserId() + " and " + SCANPOINT_ID + " = " +
                        scanPoint.getScanPointId() + " and " + TEAM_ID + " = " + team.getTeamId() +
                        " and " + TEAMUSER_ID + " = " + TEMPLATE_TEAMUSER_ID;
        String insertSql =
                "insert into " + TABLE_RAW_TEAM_LEVEL_DISMISS + "(" + DISMISS_DATE + ", " +
                        DEVICE_ID + ", " + USER_ID + ", " + SCANPOINT_ID + ", " + TEAM_ID + ", " +
                        TEAMUSER_ID + ") values (?, " + Settings.getInstance().getDeviceId() + ", " +
                        Settings.getInstance().getUserId() + ", " + scanPoint.getScanPointId() + ", " +
                        team.getTeamId() + ", ?)";
        db.beginTransaction();
        try {
            for (Participant member : dismissedMembers) {
                if (isRecordExists(selectSql, member)) continue;
                db.execSQL(insertSql, new Object[]{DateFormat.format(recordDateTime), new Integer(member.getUserId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private boolean isRecordExists(String selectSql, Participant member) {
        Cursor resultCursor =
                db.rawQuery(selectSql.replace(TEMPLATE_TEAMUSER_ID, Integer.toString(member.getUserId())), null);
        resultCursor.moveToFirst();
        int recordCount = resultCursor.getInt(0);
        resultCursor.close();
        return recordCount > 0;
    }

    public List<RawTeamLevelDismiss> loadDismissedMembers(ScanPoint scanPoint) {
        List<RawTeamLevelDismiss> result = new ArrayList<RawTeamLevelDismiss>();
        String sql =
                "select distinct d." + DISMISS_DATE + ", d." + TEAM_ID + ", d." + TEAMUSER_ID +
                        ", sp." + SCANPOINT_ORDER + " from " + TABLE_RAW_TEAM_LEVEL_DISMISS +
                        " as d join " + TABLE_SCANPOINTS + " as sp on (d." + SCANPOINT_ID + " = sp." +
                        SCANPOINT_ID + ") where sp." + SCANPOINT_ORDER + " <= " +
                        scanPoint.getScanPointOrder();
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
            int teamId = resultCursor.getInt(1);
            int teamUserId = resultCursor.getInt(2);
            int dbScanPointOrder = resultCursor.getInt(3);
            if (isDBScanPointEarlier(scanPoint, dbScanPointOrder)) {
                RawTeamLevelDismiss teamDismiss =
                        new RawTeamLevelDismiss(scanPoint.getScanPointId(), teamId, teamUserId, recordDateTime);
                // init reference fields
                teamDismiss.setScanPoint(scanPoint);
                teamDismiss.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
                teamDismiss.setTeamUser(UsersRegistry.getInstance().getUserById(teamUserId));
                result.add(teamDismiss);
            }
            resultCursor.moveToNext();
        }
        resultCursor.close();

        return result;
    }

    public void appendScanPointTeams(ScanPoint scanPoint, Set<Integer> teams) {
        String sql =
                "select distinct " + TEAM_ID + " from " + TABLE_RAW_TEAM_LEVEL_DISMISS + " where " +
                        SCANPOINT_ID + " = " + scanPoint.getScanPointId();
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            teams.add(resultCursor.getInt(0));
            resultCursor.moveToNext();
        }
        resultCursor.close();
    }

    public List<RawTeamLevelDismiss> loadAllDismissedMembersForDevice() {
        List<RawTeamLevelDismiss> result = new ArrayList<RawTeamLevelDismiss>();
        int deviceId = Settings.getInstance().getDeviceId();
        String sql =
                "select distinct d." + USER_ID + ", d." + DISMISS_DATE + ", d." + TEAM_ID + ", d." + TEAMUSER_ID +
                        ", d." + SCANPOINT_ID + " from " + TABLE_RAW_TEAM_LEVEL_DISMISS +
                        " as d where d." + DEVICE_ID + " = " + deviceId;
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            int userId = resultCursor.getInt(0);
            Date recordDateTime = DateFormat.parse(resultCursor.getString(1));
            int teamId = resultCursor.getInt(2);
            int teamUserId = resultCursor.getInt(3);
            int scanPointId = resultCursor.getInt(4);
            RawTeamLevelDismiss teamDismiss =
                    new RawTeamLevelDismiss(userId, deviceId, scanPointId, teamId, teamUserId, recordDateTime);
            // init reference fields
            teamDismiss.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanPointId));
            teamDismiss.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
            teamDismiss.setTeamUser(UsersRegistry.getInstance().getUserById(teamUserId));
            result.add(teamDismiss);
            resultCursor.moveToNext();
        }
        resultCursor.close();

        return result;
    }
}
