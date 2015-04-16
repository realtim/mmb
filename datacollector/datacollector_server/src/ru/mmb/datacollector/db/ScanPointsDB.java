package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.ScanPoint;

public class ScanPointsDB {
	private static final Logger logger = LogManager.getLogger(ScanPointsDB.class);

	private static final String TABLE_SCANPOINTS = "ScanPoints";
	private static final String RAID_ID = "raid_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String SCANPOINT_NAME = "scanpoint_name";
	private static final String SCANPOINT_ORDER = "scanpoint_order";

	public static List<ScanPoint> loadScanPoints(int raidId) {
		List<ScanPoint> result = new ArrayList<ScanPoint>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + SCANPOINT_ID + "`, `" + SCANPOINT_NAME + "`, `" + SCANPOINT_ORDER
						+ "` from `" + TABLE_SCANPOINTS + "` where `" + RAID_ID + "` = " + raidId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int scanPointId = rs.getInt(1);
					String scanPointName = rs.getString(2);
					int scanPointOrder = rs.getInt(3);
					result.add(new ScanPoint(scanPointId, raidId, scanPointName, scanPointOrder));
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
