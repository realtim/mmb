package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;

public class RawLoggerDataDB {
	private static final Logger logger = LogManager.getLogger(RawLoggerDataDB.class);

	private static final String TABLE_RAW_LOGGER_DATA = "RawLoggerData";

	private static final String USER_ID = "user_id";
	private static final String DEVICE_ID = "device_id";
	private static final String LOGGER_ID = "logger_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String RAWLOGGERDATA_DATE = "rawloggerdata_date";

	public static synchronized List<RawLoggerData> loadRawLoggerData(int scanPointId) {
		List<RawLoggerData> result = new ArrayList<RawLoggerData>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select t.`" + USER_ID + "`, t.`" + DEVICE_ID + "`, t.`" + LOGGER_ID + "`, t.`" + TEAM_ID
						+ "`, t.`" + RAWLOGGERDATA_DATE + "` from `" + TABLE_RAW_LOGGER_DATA + "` as t " + " where t.`"
						+ SCANPOINT_ID + "` = " + scanPointId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int userId = rs.getInt(1);
					int deviceId = rs.getInt(2);
					int loggerId = rs.getInt(3);
					int teamId = rs.getInt(4);
					String recordDate = rs.getString(5);

					RawLoggerData rawLoggerData = new RawLoggerData(userId, deviceId, loggerId, scanPointId, teamId,
							DateFormat.parse(recordDate));
					// init reference fields
					rawLoggerData.setScanPoint(ScanPointsRegistry.getInstance().getScanPointById(scanPointId));
					rawLoggerData.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));

					result.add(rawLoggerData);
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
