package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.model.Distance;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Distances
{
	private static final String TABLE_DISTANCES = "Distances";
	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";
	private static final String DISTANCE_NAME = "distance_name";

	private final SQLiteDatabase db;

	public Distances(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<Distance> loadDistances(int raidId)
	{
		List<Distance> result = new ArrayList<Distance>();
		String whereCondition = RAID_ID + " = " + raidId;
		Cursor resultCursor =
		    db.query(TABLE_DISTANCES, new String[] { DISTANCE_ID, DISTANCE_NAME }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int distanceId = resultCursor.getInt(0);
			String distanceName = resultCursor.getString(1);
			result.add(new Distance(distanceId, raidId, distanceName));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public static void performTestQuery(SQLiteDatabase dbToCheck)
	{
		Cursor resultCursor =
		    dbToCheck.query(TABLE_DISTANCES, new String[] { DISTANCE_ID, DISTANCE_NAME }, null, null, null, null, null);
		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			resultCursor.moveToNext();
		}
		resultCursor.close();
	}
}
