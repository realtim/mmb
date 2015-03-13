package ru.mmb.datacollector.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.PointType;
import ru.mmb.datacollector.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class LevelPoints
{
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

	private final SQLiteDatabase db;

	private List<LevelPoint> levelPoints;
	private List<Checkpoint> currentCheckpoints;

	public LevelPoints(SQLiteDatabase db)
	{
		this.db = db;
	}

	/**
	 * Load all records from LevelPoints table ordered by DISTANCE_ID,
	 * LEVELPOINT_ID. Process each levelpoint. Ordering is correct, no local
	 * array sorts needed. Create levelpoint or checkpoint. Add current list of
	 * checkpoints to following finish point.
	 * 
	 * @param raidId
	 *            Current raid id.
	 * @return List of loaded levelpoints. Distances and scanpoints must be
	 *         attached to them immediately after load.
	 */
	public List<LevelPoint> loadLevelPoints(int raidId)
	{
		levelPoints = new ArrayList<LevelPoint>();
		currentCheckpoints = new ArrayList<Checkpoint>();

		// @formatter:off
		String sql =
		    "select lp." + LEVELPOINT_ID + ", " + 
		    		"lp." + POINTTYPE_ID + ", " + 
		    		"lp." + DISTANCE_ID + ", " + 
		    		"lp." + SCANPOINT_ID + ", " + 
		    		"lp." + LEVELPOINT_ORDER + ", " + 
		    		"lp." + LEVELPOINT_NAME + ", " +
		    		"lp." + LEVELPOINT_PENALTY + ", " + 
		    		"lp." + LEVELPOINT_MINDATETIME + ", " + 
		    		"lp." + LEVELPOINT_MAXDATETIME + " " + 
		    "from " + TABLE_LEVEL_POINTS + " lp join " + TABLE_DISTANCES + " d " + 
		    		"on (lp." + DISTANCE_ID + " = d." + DISTANCE_ID + ") " + 
		    "where d." + RAID_ID + " = " + raidId + " " +
		    "order by lp." + DISTANCE_ID + ", lp." + LEVELPOINT_ORDER;
		// @formatter:on
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int levelPointId = resultCursor.getInt(0);
			int pointTypeId = resultCursor.getInt(1);
			PointType pointType = PointType.getById(pointTypeId);
			int distanceId = resultCursor.getInt(2);
			int scanPointId = resultCursor.getInt(3);
			int levelPointOrder = resultCursor.getInt(4);
			String levelPointName = resultCursor.getString(5);
			int levelPointPenalty = resultCursor.getInt(6);
			Date levelPointMinDateTime = null;
			if (!pointType.isCheckpoint())
			{
				levelPointMinDateTime = DateFormat.parse(resultCursor.getString(7));
			}
			Date levelPointMaxDateTime = null;
			if (!pointType.isCheckpoint())
			{
				levelPointMaxDateTime = DateFormat.parse(resultCursor.getString(8));
			}

			addLevelPointToResult(levelPointId, pointType, distanceId, scanPointId, levelPointOrder, levelPointName, levelPointPenalty, levelPointMinDateTime, levelPointMaxDateTime);

			resultCursor.moveToNext();
		}
		resultCursor.close();

		return new ArrayList<LevelPoint>(levelPoints);
	}

	/**
	 * Add finish and start points to levelPoints array. Add checkpoint to
	 * currentCheckpoints list.
	 * 
	 * If adding finish point append current checkpoints to finish point. Clear
	 * current checkpoints list.
	 */
	private void addLevelPointToResult(int levelPointId, PointType pointType, int distanceId,
	        int scanPointId, int levelPointOrder, String levelPointName, int levelPointPenalty,
	        Date levelPointMinDateTime, Date levelPointMaxDateTime)
	{
		if (pointType.isFinish())
		{
			LevelPoint finishPoint =
			    new LevelPoint(levelPointId, pointType, distanceId, scanPointId, levelPointOrder, levelPointMinDateTime, levelPointMaxDateTime);
			addCheckpointsTo(finishPoint);
			currentCheckpoints.clear();
			levelPoints.add(finishPoint);
		}
		else if (pointType.isStart())
		{
			LevelPoint startPoint =
			    new LevelPoint(levelPointId, pointType, distanceId, scanPointId, levelPointOrder, levelPointMinDateTime, levelPointMaxDateTime);
			levelPoints.add(startPoint);
		}
		else if (pointType.isCheckpoint())
		{
			Checkpoint checkpoint =
			    new Checkpoint(levelPointId, levelPointOrder, levelPointName, levelPointPenalty);
			currentCheckpoints.add(checkpoint);
		}
	}

	private void addCheckpointsTo(LevelPoint finishPoint)
	{
		for (Checkpoint checkpoint : currentCheckpoints)
		{
			finishPoint.addCheckpoint(checkpoint);
			checkpoint.setLevelPoint(finishPoint);
		}
	}
}
