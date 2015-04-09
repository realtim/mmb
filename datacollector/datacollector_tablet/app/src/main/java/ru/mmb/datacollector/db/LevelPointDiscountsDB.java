package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.model.LevelPointDiscount;

public class LevelPointDiscountsDB
{
	private static final String TABLE_DISTANCES = "Distances";
	private static final String RAID_ID = "raid_id";
	private static final String DISTANCE_ID = "distance_id";

	private static final String TABLE_LEVELPOINTDISCOUNTS = "LevelPointDiscounts";
	private static final String LEVELPOINTDISCOUNT_ID = "levelpointdiscount_id";
	private static final String LEVELPOINTDISCOUNT_VALUE = "levelpointdiscount_value";
	private static final String LEVELPOINTDISCOUNT_START = "levelpointdiscount_start";
	private static final String LEVELPOINTDISCOUNT_FINISH = "levelpointdiscount_finish";

	private final SQLiteDatabase db;

	public LevelPointDiscountsDB(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId)
	{
		List<LevelPointDiscount> result = new ArrayList<LevelPointDiscount>();
		// @formatter:off
		String sql =
		    "select lpd." + LEVELPOINTDISCOUNT_ID + ", " + 
		    		"lpd." + DISTANCE_ID + ", " + 
		    		"lpd." + LEVELPOINTDISCOUNT_VALUE + ", " + 
		    		"lpd." + LEVELPOINTDISCOUNT_START + ", " + 
		    		"lpd." + LEVELPOINTDISCOUNT_FINISH + " " + 
		    "from " + TABLE_LEVELPOINTDISCOUNTS + " lpd join " + TABLE_DISTANCES + " d " + 
		    		"on (lpd." + DISTANCE_ID + " = d." + DISTANCE_ID + ") " + 
		    "where d." + RAID_ID + " = " + raidId;
		// @formatter:on
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int levelPointDiscountId = resultCursor.getInt(0);
			int distanceId = resultCursor.getInt(1);
			int levelPointDiscountValue = resultCursor.getInt(2);
			int levelPointDiscountStart = resultCursor.getInt(3);
			int levelPointDiscountFinish = resultCursor.getInt(4);
			result.add(new LevelPointDiscount(levelPointDiscountId, distanceId, levelPointDiscountValue, levelPointDiscountStart, levelPointDiscountFinish));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}
}
