package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.terminal.model.BarCodeScan;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import ru.mmb.terminal.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BarCodeScans
{
	private static final String TABLE_BAR_CODE_SCANS = "BarCodeScans";

	private static final String BARCODESCAN_DATE = "barcodescan_date";
	private static final String DEVICE_ID = "device_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMLEVELPOINT_DATETIME = "teamlevelpoint_datetime";

	private final SQLiteDatabase db;

	public BarCodeScans(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<BarCodeScan> loadBarCodeScans(LevelPoint levelPoint)
	{
		List<BarCodeScan> result = new ArrayList<BarCodeScan>();
		String sql =
		    "select " + BARCODESCAN_DATE + ", " + DEVICE_ID + ", " + TEAM_ID + ", "
		            + TEAMLEVELPOINT_DATETIME + " from " + TABLE_BAR_CODE_SCANS + " where "
		            + LEVELPOINT_ID + " = " + levelPoint.getLevelPointId();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
			Integer deviceId = resultCursor.getInt(1);
			Integer teamId = resultCursor.getInt(2);
			Date checkDateTime = DateFormat.parse(resultCursor.getString(3));

			BarCodeScan barCodeScan =
			    new BarCodeScan(teamId, deviceId, levelPoint.getLevelPointId(), checkDateTime, recordDateTime);
			// init reference fields
			barCodeScan.setLevelPoint(levelPoint);
			barCodeScan.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));

			result.add(barCodeScan);
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public void saveBarCodeScan(LevelPoint levelPoint, Team team, Date checkDateTime,
	        Date recordDateTime)
	{
		db.beginTransaction();
		try
		{
			if (isThisRecordExists(levelPoint, team))
			{
				updateExistingRecord(levelPoint, team, checkDateTime, recordDateTime);
			}
			else
			{
				insertNewRecord(levelPoint, team, checkDateTime, recordDateTime);
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	private boolean isThisRecordExists(LevelPoint levelPoint, Team team)
	{
		String selectSql =
		    "select count(*) from " + TABLE_BAR_CODE_SCANS + " where " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
		Cursor resultCursor = db.rawQuery(selectSql, null);
		resultCursor.moveToFirst();
		int recordCount = resultCursor.getInt(0);
		resultCursor.close();
		return recordCount > 0;
	}

	private void updateExistingRecord(LevelPoint levelPoint, Team team, Date checkDateTime,
	        Date recordDateTime)
	{
		String updateSql =
		    "update " + TABLE_BAR_CODE_SCANS + " set " + DEVICE_ID + " = "
		            + Settings.getInstance().getDeviceId() + ", " + BARCODESCAN_DATE + " = '"
		            + DateFormat.format(recordDateTime) + "', " + TEAMLEVELPOINT_DATETIME + " = '"
		            + DateFormat.format(checkDateTime) + "'" + " where " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
		db.execSQL(updateSql);
	}

	private void insertNewRecord(LevelPoint levelPoint, Team team, Date checkDateTime,
	        Date recordDateTime)
	{
		String insertSql =
		    "insert into " + TABLE_BAR_CODE_SCANS + " (" + DEVICE_ID + ", " + LEVELPOINT_ID + ", "
		            + TEAM_ID + ", " + BARCODESCAN_DATE + ", " + TEAMLEVELPOINT_DATETIME
		            + ") values (" + Settings.getInstance().getDeviceId() + ", "
		            + levelPoint.getLevelPointId() + ", " + team.getTeamId() + ", '"
		            + DateFormat.format(recordDateTime) + "', '" + DateFormat.format(checkDateTime)
		            + "')";
		db.execSQL(insertSql);
	}
}
