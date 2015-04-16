package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.Distance;

public class DistancesDB {
	private static final Logger logger = LogManager.getLogger(DistancesDB.class);

	private static final String TABLE_DISTANCES = "Distances";
	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String DISTANCE_NAME = "distance_name";

	public static synchronized List<Distance> loadDistances(int raidId) {
		List<Distance> result = new ArrayList<Distance>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + DISTANCE_ID + "`, `" + DISTANCE_NAME + "` from `" + TABLE_DISTANCES
						+ "` where `" + RAID_ID + "` = " + raidId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int distanceId = rs.getInt(1);
					String distanceName = rs.getString(2);
					result.add(new Distance(distanceId, raidId, distanceName));
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

	public static boolean checkConnectionAlive() {
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			PreparedStatement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select count(`" + DISTANCE_ID + "`) from `" + TABLE_DISTANCES + "`";
				stmt = conn.prepareStatement(sql);
				rs = stmt.executeQuery();
				if (rs.next()) {
					return true;
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

		return false;
	}
}
