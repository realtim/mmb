package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class TeamResultsDB {
    private static final String TABLE_TEAM_LEVEL_POINTS = "TeamLevelPoints";

    private static final String TEAMLEVELPOINT_DATE = "teamlevelpoint_date";
    private static final String DEVICE_ID = "device_id";
    private static final String USER_ID = "user_id";
    private static final String LEVELPOINT_ID = "levelpoint_id";
    private static final String TEAM_ID = "team_id";
    private static final String TEAMLEVELPOINT_DATETIME = "teamlevelpoint_datetime";
    private static final String TEAMLEVELPOINT_POINTS = "teamlevelpoint_points";
    private static final String TEAMLEVELPOINT_COMMENT = "teamlevelpoint_comment";

    private final SQLiteDatabase db;

    public TeamResultsDB(SQLiteDatabase db) {
        this.db = db;
    }

    public List<TeamResult> loadTeamResults(Team team) {
        List<TeamResult> result = new ArrayList<TeamResult>();
        String sql =
                "select " + TEAMLEVELPOINT_DATE + ", " + USER_ID + ", " + DEVICE_ID + ", " + TEAM_ID
                + ", " + LEVELPOINT_ID + ", " + TEAMLEVELPOINT_DATETIME + ", "
                + TEAMLEVELPOINT_POINTS + " from " + TABLE_TEAM_LEVEL_POINTS + " where "
                + TEAM_ID + " = " + team.getTeamId() + " order by " + LEVELPOINT_ID + ", "
                + TEAMLEVELPOINT_DATE;
        Cursor resultCursor = db.rawQuery(sql, null);

        resultCursor.moveToFirst();
        while (!resultCursor.isAfterLast()) {
            Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
            Integer userId = resultCursor.getInt(1);
            Integer deviceId = resultCursor.getInt(2);
            Integer teamId = resultCursor.getInt(3);
            int levelPointId = resultCursor.getInt(4);
            Date checkDateTime = DateFormat.parse(resultCursor.getString(5));
            String takenCheckpointNames = replaceNullWithEmptyString(resultCursor.getString(6));

            ScanPoint scanPoint =
                    ScanPointsRegistry.getInstance().getScanPointByLevelPointId(levelPointId);

            TeamResult teamResult =
                    new TeamResult(teamId, userId, deviceId, scanPoint.getScanPointId(), takenCheckpointNames, checkDateTime, recordDateTime);
            // init reference fields
            teamResult.setScanPoint(scanPoint);
            teamResult.setTeam(team);
            teamResult.initTakenCheckpoints();

            result.add(teamResult);
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
}
