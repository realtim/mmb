package ru.mmb.datacollector.db;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.TeamLevelPoints;
import ru.mmb.datacollector.util.DateFormat;

public class TeamLevelPointsDB {
	private static final String TABLE_TEAM_LEVEL_POINTS = "TeamLevelPoints";

	private static final String TEAMLEVELPOINT_DATE = "teamlevelpoint_date";
	private static final String DEVICE_ID = "device_id";
	private static final String USER_ID = "user_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMLEVELPOINT_DATETIME = "teamlevelpoint_datetime";
	private static final String TEAMLEVELPOINT_POINTS = "teamlevelpoint_points";
	private static final String TEAMLEVELPOINT_COMMENT = "teamlevelpoint_comment";

	public static synchronized String getTeamLevelPointsInsertSql(TeamLevelPoints teamLevelPoints) {
		int distanceId = teamLevelPoints.getTeam().getDistanceId();
		LevelPoint levelPoint = teamLevelPoints.getScanPoint().getLevelPointByDistance(distanceId);
		String sql = "insert into `" + TABLE_TEAM_LEVEL_POINTS + "`(`" + TEAMLEVELPOINT_DATE + "`, `" + USER_ID
				+ "`, `" + DEVICE_ID + "`, " + LEVELPOINT_ID + "`, `" + TEAM_ID + "`, `" + TEAMLEVELPOINT_DATETIME
				+ "`, `" + TEAMLEVELPOINT_POINTS + "`, `" + TEAMLEVELPOINT_COMMENT + "`) " + "values('"
				+ DateFormat.format(teamLevelPoints.getRecordDateTime()) + "', " + teamLevelPoints.getUserId() + ", "
				+ teamLevelPoints.getDeviceId() + ", " + levelPoint.getLevelPointId() + ", "
				+ teamLevelPoints.getTeamId() + ", '" + DateFormat.format(teamLevelPoints.getCheckDateTime()) + "', '"
				+ teamLevelPoints.getTakenCheckpointNames() + "', '')";
		return sql;
	}
}
