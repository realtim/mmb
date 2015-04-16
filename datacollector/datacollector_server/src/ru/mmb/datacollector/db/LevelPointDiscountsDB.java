package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.LevelPointDiscount;

public class LevelPointDiscountsDB {
	private static final Logger logger = LogManager.getLogger(LevelPointDiscountsDB.class);

	private static final String TABLE_DISTANCES = "Distances";
	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";

	private static final String TABLE_LEVELPOINTDISCOUNTS = "LevelPointDiscounts";
	private static final String LEVELPOINTDISCOUNT_ID = "levelpointdiscount_id";
	private static final String LEVELPOINTDISCOUNT_VALUE = "levelpointdiscount_value";
	private static final String LEVELPOINTDISCOUNT_START = "levelpointdiscount_start";
	private static final String LEVELPOINTDISCOUNT_FINISH = "levelpointdiscount_finish";

	public static synchronized List<LevelPointDiscount> loadLevelPointDiscounts(int raidId) {
		List<LevelPointDiscount> result = new ArrayList<LevelPointDiscount>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select lpd.`" + LEVELPOINTDISCOUNT_ID + "`, " + "lpd.`" + DISTANCE_ID + "`, " + "lpd.`"
						+ LEVELPOINTDISCOUNT_VALUE + "`, " + "lpd.`" + LEVELPOINTDISCOUNT_START + "`, " + "lpd.`"
						+ LEVELPOINTDISCOUNT_FINISH + "` " + "from `" + TABLE_LEVELPOINTDISCOUNTS + "` lpd join `"
						+ TABLE_DISTANCES + "` d " + "on (lpd.`" + DISTANCE_ID + "` = d.`" + DISTANCE_ID + "`) "
						+ "where d.`" + RAID_ID + "` = " + raidId;
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int levelPointDiscountId = rs.getInt(1);
					int distanceId = rs.getInt(2);
					int levelPointDiscountValue = rs.getInt(3);
					int levelPointDiscountStart = rs.getInt(4);
					int levelPointDiscountFinish = rs.getInt(5);
					result.add(new LevelPointDiscount(levelPointDiscountId, distanceId, levelPointDiscountValue,
							levelPointDiscountStart, levelPointDiscountFinish));
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
