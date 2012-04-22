package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.PointType;
import ru.mmb.terminal.model.StartType;
import ru.mmb.terminal.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Levels
{
	private static final String TABLE_LEVELS = "Levels";
	private static final String TABLE_LEVEL_POINTS = "LevelPoints";

	private static final String LEVEL_ID = "level_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String LEVEL_NAME = "level_name";
	private static final String LEVEL_ORDER = "level_order";
	private static final String LEVEL_STARTTYPE = "level_starttype";
	private static final String LEVEL_POINTNAMES = "level_pointnames";
	private static final String LEVEL_POINTPENALTIES = "level_pointpenalties";
	private static final String LEVEL_BEGTIME = "level_begtime";
	private static final String LEVEL_MAXBEGTIME = "level_maxbegtime";
	private static final String LEVEL_MINENDTIME = "level_minendtime";
	private static final String LEVEL_ENDTIME = "level_endtime";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String POINTTYPE_ID = "pointtype_id";

	private final SQLiteDatabase db;

	public Levels(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<Level> loadLevels(int distanceId)
	{
		List<Level> result = new ArrayList<Level>();
		String whereCondition = DISTANCE_ID + " = " + distanceId;
		Cursor resultCursor =
		    db.query(TABLE_LEVELS, new String[] { LEVEL_ID, LEVEL_NAME, LEVEL_ORDER,
		            LEVEL_STARTTYPE, LEVEL_POINTNAMES, LEVEL_POINTPENALTIES, LEVEL_BEGTIME,
		            LEVEL_MAXBEGTIME, LEVEL_MINENDTIME, LEVEL_ENDTIME }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int levelId = resultCursor.getInt(0);
			String levelName = resultCursor.getString(1);
			int levelOrder = resultCursor.getInt(2);
			StartType levelStartType = StartType.getTypeById(resultCursor.getInt(3));
			String levelPointNames = resultCursor.getString(4);
			String levelPointPenalties = resultCursor.getString(5);
			Date levelBegTime = DateFormat.parse(resultCursor.getString(6));
			Date levelMaxBegTime = DateFormat.parse(resultCursor.getString(7));
			Date levelMinEndTime = DateFormat.parse(resultCursor.getString(8));
			Date levelEndTime = DateFormat.parse(resultCursor.getString(9));

			Level newLevel =
			    new Level(levelId, distanceId, levelName, levelOrder, levelStartType, levelBegTime, levelMaxBegTime, levelMinEndTime, levelEndTime);
			newLevel.addCheckpoints(levelPointNames, levelPointPenalties);
			result.add(newLevel);
			resultCursor.moveToNext();
		}
		resultCursor.close();

		loadStartAndFinishPoints(result);

		return result;
	}

	private void loadStartAndFinishPoints(List<Level> levels)
	{
		loadStartPoints(levels);
		loadFinishPoints(levels);
	}

	private void loadStartPoints(List<Level> levels)
	{
		Map<Integer, LevelPoint> result = loadLevelPointsOfType(PointType.START);

		for (Level level : levels)
		{
			LevelPoint levelPoint = result.get(level.getLevelId());
			level.setStartPoint(levelPoint);
			levelPoint.setLevel(level);
		}
	}

	private Map<Integer, LevelPoint> loadLevelPointsOfType(PointType pointType)
	{
		Map<Integer, LevelPoint> result = new HashMap<Integer, LevelPoint>();
		String whereCondition = POINTTYPE_ID + " = " + pointType.getId();
		Cursor resultCursor =
		    db.query(TABLE_LEVEL_POINTS, new String[] { LEVELPOINT_ID, LEVEL_ID }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int levelPointId = resultCursor.getInt(0);
			int levelId = resultCursor.getInt(1);

			result.put(levelId, new LevelPoint(pointType, levelPointId, levelId));
			resultCursor.moveToNext();
		}
		resultCursor.close();
		return result;
	}

	private void loadFinishPoints(List<Level> levels)
	{
		Map<Integer, LevelPoint> result = loadLevelPointsOfType(PointType.FINISH);

		for (Level level : levels)
		{
			LevelPoint levelPoint = result.get(level.getLevelId());
			level.setFinishPoint(levelPoint);
			levelPoint.setLevel(level);
		}
	}
}
