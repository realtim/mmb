package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.model.ScanPoint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ScanPoints
{
	private static final String TABLE_SCANPOINTS = "ScanPoints";
	private static final String RAID_ID = "raid_id";
	private static final String SCANPOINT_ID = "scanpoint_id";
	private static final String SCANPOINT_NAME = "scanpoint_name";
	private static final String SCANPOINT_ORDER = "scanpoint_order";

	private final SQLiteDatabase db;

	public ScanPoints(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<ScanPoint> loadScanPoints(int raidId)
	{
		List<ScanPoint> result = new ArrayList<ScanPoint>();
		String whereCondition = RAID_ID + " = " + raidId;
		Cursor resultCursor =
		    db.query(TABLE_SCANPOINTS, new String[] { SCANPOINT_ID, SCANPOINT_NAME, SCANPOINT_ORDER }, whereCondition, null, null, null, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int scanPointId = resultCursor.getInt(0);
			String scanPointName = resultCursor.getString(1);
			int scanPointOrder = resultCursor.getInt(2);
			result.add(new ScanPoint(scanPointId, raidId, scanPointName, scanPointOrder));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}
}
