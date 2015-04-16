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

import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.PointType;
import ru.mmb.datacollector.util.DateFormat;

public class LevelPointsDB {
	private static final Logger logger = LogManager.getLogger(LevelPointsDB.class);

	private static final String TABLE_DISTANCES = "Distances";
	private static final String TABLE_LEVEL_POINTS = "LevelPoints";

	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String POINTTYPE_ID = "pointtype_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String LEVELPOINT_NAME = "levelpoint_name";
	private static final String LEVELPOINT_ORDER = "levelpoint_order";
	private static final String LEVELPOINT_PENALTY = "levelpoint_penalty";
	private static final String LEVELPOINT_MINDATETIME = "levelpoint_mindatetime";
	private static final String LEVELPOINT_MAXDATETIME = "levelpoint_maxdatetime";

	private static List<LevelPoint> levelPoints;
	private static List<Checkpoint> currentCheckpoints;

	public static synchronized List<LevelPoint> loadLevelPoints(int raidId) {
		levelPoints = new ArrayList<LevelPoint>();
		currentCheckpoints = new ArrayList<Checkpoint>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select lp.`" + LEVELPOINT_ID + "`, " + "lp.`" + POINTTYPE_ID + "`, " + "lp.`"
						+ DISTANCE_ID + "`, " + "lp.`" + SCANPOINT_ID + "`, " + "lp.`" + LEVELPOINT_ORDER + "`, "
						+ "lp.`" + LEVELPOINT_NAME + "`, " + "lp.`" + LEVELPOINT_PENALTY + "`, " + "lp.`"
						+ LEVELPOINT_MINDATETIME + "`, " + "lp.`" + LEVELPOINT_MAXDATETIME + "` " + "from `"
						+ TABLE_LEVEL_POINTS + "` lp join `" + TABLE_DISTANCES + "` d " + "on (lp.`" + DISTANCE_ID
						+ "` = d.`" + DISTANCE_ID + "`) " + "where d.`" + RAID_ID + "` = " + raidId + " "
						+ "order by lp.`" + DISTANCE_ID + "`, lp.`" + LEVELPOINT_ORDER + "`";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int levelPointId = rs.getInt(1);
					int pointTypeId = rs.getInt(2);
					PointType pointType = PointType.getById(pointTypeId);
					int distanceId = rs.getInt(3);
					int scanPointId = rs.getInt(4);
					int levelPointOrder = rs.getInt(5);
					String levelPointName = rs.getString(6);
					int levelPointPenalty = rs.getInt(7);
					Date levelPointMinDateTime = null;
					if (!pointType.isCheckpoint()) {
						levelPointMinDateTime = DateFormat.parse(rs.getString(8));
					}
					Date levelPointMaxDateTime = null;
					if (!pointType.isCheckpoint()) {
						levelPointMaxDateTime = DateFormat.parse(rs.getString(9));
					}
					addLevelPointToResult(levelPointId, pointType, distanceId, scanPointId, levelPointOrder,
							levelPointName, levelPointPenalty, levelPointMinDateTime, levelPointMaxDateTime);
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

		return new ArrayList<LevelPoint>(levelPoints);
	}

	private static void addLevelPointToResult(int levelPointId, PointType pointType, int distanceId, int scanPointId,
			int levelPointOrder, String levelPointName, int levelPointPenalty, Date levelPointMinDateTime,
			Date levelPointMaxDateTime) {
		if (pointType.isFinish()) {
			LevelPoint finishPoint = new LevelPoint(levelPointId, pointType, distanceId, scanPointId, levelPointOrder,
					levelPointMinDateTime, levelPointMaxDateTime);
			addCheckpointsTo(finishPoint);
			currentCheckpoints.clear();
			levelPoints.add(finishPoint);
		} else if (pointType.isStart()) {
			LevelPoint startPoint = new LevelPoint(levelPointId, pointType, distanceId, scanPointId, levelPointOrder,
					levelPointMinDateTime, levelPointMaxDateTime);
			levelPoints.add(startPoint);
		} else if (pointType.isCheckpoint()) {
			Checkpoint checkpoint = new Checkpoint(levelPointId, levelPointOrder, levelPointName, levelPointPenalty);
			currentCheckpoints.add(checkpoint);
		}
	}

	private static void addCheckpointsTo(LevelPoint finishPoint) {
		for (Checkpoint checkpoint : currentCheckpoints) {
			finishPoint.addCheckpoint(checkpoint);
			checkpoint.setLevelPoint(finishPoint);
		}
	}
}
