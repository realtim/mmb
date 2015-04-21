package ru.mmb.datacollector.db;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.TeamLevelDismiss;
import ru.mmb.datacollector.util.DateFormat;

public class TeamLevelDismissDB {
	private static final String TABLE_TEAM_LEVEL_DISMISS = "TeamLevelDismiss";

	private static final String USER_ID = "user_id";
	private static final String DEVICE_ID = "device_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMUSER_ID = "teamuser_id";
	private static final String DISMISS_DATE = "teamleveldismiss_date";

	public static synchronized String getTeamLevelDismissInsertSql(TeamLevelDismiss teamLevelDismiss) {
		int distanceId = teamLevelDismiss.getTeam().getDistanceId();
		LevelPoint levelPoint = teamLevelDismiss.getScanPoint().getLevelPointByDistance(distanceId);
		String sql = "insert into `" + TABLE_TEAM_LEVEL_DISMISS + "`(`" + USER_ID + "`, `" + DEVICE_ID + "`, "
				+ LEVELPOINT_ID + "`, `" + TEAM_ID + "`, `" + TEAMUSER_ID + "`, `" + DISMISS_DATE + "`) " + "values("
				+ teamLevelDismiss.getUserId() + ", " + teamLevelDismiss.getDeviceId() + ", "
				+ levelPoint.getLevelPointId() + ", " + teamLevelDismiss.getTeamId() + ", "
				+ teamLevelDismiss.getTeamUserId() + ", '" + DateFormat.format(teamLevelDismiss.getRecordDateTime())
				+ "')";
		return sql;
	}
}
