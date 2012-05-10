package ru.mmb.terminal.db;

import java.util.Date;

import ru.mmb.terminal.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Raids
{
	private static final String TABLE_RAIDS = "Raids";

	private static final String RAID_ID = "raid_id";
	private static final String RAID_REGISTRATIONENDDATE = "raid_registrationenddate";

	private final SQLiteDatabase db;

	public Raids(SQLiteDatabase db)
	{
		this.db = db;
	}

	public int getCurrentRaidId()
	{
		Cursor resultCursor =
		    db.query(TABLE_RAIDS, new String[] { RAID_ID, RAID_REGISTRATIONENDDATE }, null, null, null, null, null);

		Date maxRegistrationDate = null;
		int result = -1;
		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int raidId = resultCursor.getInt(0);
			if (!resultCursor.isNull(1))
			{
				Date raidRegistrationEndDate = DateFormat.parse(resultCursor.getString(1));
				if (maxRegistrationDate == null
				        || maxRegistrationDate.before(raidRegistrationEndDate))
				{
					maxRegistrationDate = raidRegistrationEndDate;
					result = raidId;
				}
			}
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}
}
