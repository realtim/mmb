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

import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawTeamLevelPointsDB {
	private static final Logger logger = LogManager.getLogger(RawTeamLevelPointsDB.class);

	private static final String TABLE_RAW_TEAM_LEVEL_POINTS = "RawTeamLevelPoints";

	private static final String USER_ID = "user_id";
	private static final String DEVICE_ID = "device_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String RAWTEAMLEVELPOINTS_POINTS = "rawteamlevelpoints_points";
	private static final String RAWTEAMLEVELPOINTS_DATE = "rawteamlevelpoints_date";

	public static synchronized List<RawTeamLevelPoints> loadRawTeamLevelPoints(int scanPointId) {
		List<RawTeamLevelPoints> result = new ArrayList<RawTeamLevelPoints>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + USER_ID + "`, `" + DEVICE_ID + "`, `" + TEAM_ID + "`, `"
						+ RAWTEAMLEVELPOINTS_DATE + "`, `" + RAWTEAMLEVELPOINTS_POINTS + "` from `"
						+ TABLE_RAW_TEAM_LEVEL_POINTS + "` where `" + SCANPOINT_ID + "` = " + scanPointId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Integer userId = rs.getInt(1);
					Integer deviceId = rs.getInt(2);
					Integer teamId = rs.getInt(3);
					Date recordDateTime = DateFormat.parse(rs.getString(4));
					String takenCheckpointNames = replaceNullWithEmptyString(rs.getString(5));

					RawTeamLevelPoints rawTeamLevelPoints = new RawTeamLevelPoints(teamId, userId, deviceId,
							scanPointId, takenCheckpointNames, recordDateTime);
					// init reference fields
					rawTeamLevelPoints.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanPointId));
					rawTeamLevelPoints.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
					rawTeamLevelPoints.initTakenCheckpoints();

					result.add(rawTeamLevelPoints);
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

	private static String replaceNullWithEmptyString(String takenCheckpoints) {
		if ("NULL".equals(takenCheckpoints)) {
			return "";
		} else {
			return takenCheckpoints;
		}
	}
}
