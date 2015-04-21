package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawTeamLevelDismissDB {
	private static final Logger logger = LogManager.getLogger(RawTeamLevelDismissDB.class);

	private static final String TABLE_RAW_TEAM_LEVEL_DISMISS = "RawTeamLevelDismiss";

	private static final String USER_ID = "user_id";
	private static final String DEVICE_ID = "device_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMUSER_ID = "teamuser_id";
	private static final String DISMISS_DATE = "rawteamleveldismiss_date";

	public static synchronized List<RawTeamLevelDismiss> loadRawTeamLevelDismiss(int scanPointId) {
		List<RawTeamLevelDismiss> result = new ArrayList<RawTeamLevelDismiss>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + USER_ID + "`, `" + DEVICE_ID + "`, `" + TEAM_ID + "`, `" + TEAMUSER_ID
						+ "`, `" + DISMISS_DATE + "` from `" + TABLE_RAW_TEAM_LEVEL_DISMISS + "` where `"
						+ SCANPOINT_ID + "` = " + scanPointId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Integer userId = rs.getInt(1);
					Integer deviceId = rs.getInt(2);
					Integer teamId = rs.getInt(3);
					Integer teamUserId = rs.getInt(4);
					Date recordDateTime = DateFormat.parse(rs.getString(5));

					RawTeamLevelDismiss rawTeamLevelDismiss = new RawTeamLevelDismiss(userId, deviceId, scanPointId,
							teamId, teamUserId, recordDateTime);
					// init reference fields
					rawTeamLevelDismiss.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanPointId));
					rawTeamLevelDismiss.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));

					result.add(rawTeamLevelDismiss);
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		return result;
	}
}
