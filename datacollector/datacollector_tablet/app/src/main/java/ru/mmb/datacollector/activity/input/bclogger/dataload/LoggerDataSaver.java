package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.transport.importer.Importer;
import ru.mmb.datacollector.util.DateUtils;

public class LoggerDataSaver {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");

    private final LoggerDataLoadBluetoothClient owner;
    private final SQLiteDatabase db;
    private int recordsToSave;
    private ScanPoint scanPoint;

    public LoggerDataSaver(LoggerDataLoadBluetoothClient owner) {
        this.owner = owner;
        this.db = SQLiteDatabaseAdapter.getRawInstance().getDb();
    }

    public void init() {
        recordsToSave = 0;
        db.beginTransaction();
    }

    public void flushData() {
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.d("LOGGER_DATA_SAVER", "remaining records batch commited");
    }

    public void releaseResources() {
        if (db.inTransaction()) {
            db.endTransaction();
        }
    }

    public void saveToDB(ScanPoint scanPoint, LogStringParsingResult parsingResult) {
        this.scanPoint = scanPoint;

        if (recordsToSave == 0 && !db.inTransaction()) {
            db.beginTransaction();
            Log.d("LOGGER_DATA_SAVER", "started first transaction");
        } else if (recordsToSave > 0 && recordsToSave % Importer.ROWS_IN_BATCH == 0) {
            db.setTransactionSuccessful();
            db.endTransaction();
            Log.d("LOGGER_DATA_SAVER", "records batch commited");

            db.beginTransaction();
            Log.d("LOGGER_DATA_SAVER", "started transaction");
        }

        try {
            saveRecordToDB(scanPoint, parsingResult);
        } catch (Exception e) {
            owner.writeError("ERROR in saveToDB: " + e.getMessage());
        }
    }

    private void saveRecordToDB(ScanPoint scanPoint, LogStringParsingResult parsingResult) throws Exception {
        int loggerId = Integer.parseInt(parsingResult.getLoggerId());
        int scanpointId = scanPoint.getScanPointId();
        int teamId = getTeamIdByNumber(parsingResult);
        // Remove seconds from date, or existing records will be updated when no need.
        // Dates in DB are saved without seconds, and before() or after() will return
        // undesired results, if seconds are present in parsed result.
        Date recordDateTime = DateUtils.trimToMinutes(sdf.parse(parsingResult.getRecordDateTime()));
        recordDateTime = substituteForCommonStart(recordDateTime, teamId);
        RawLoggerData existingRecord = SQLiteDatabaseAdapter.getConnectedInstance().getExistingLoggerRecord(loggerId, scanpointId, teamId);
        if (existingRecord != null) {
            if (needUpdateExistingRecord(existingRecord, recordDateTime)) {
                String sql = SQLiteDatabaseAdapter.getConnectedInstance().getUpdateExistingLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime);
                db.execSQL(sql);
                recordsToSave++;
                owner.writeToConsole("existing record updated");
            } else {
                owner.writeToConsole("existing record NOT updated");
            }
        } else {
            String sql = SQLiteDatabaseAdapter.getConnectedInstance().getInsertNewLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime);
            db.execSQL(sql);
            recordsToSave++;
            owner.writeToConsole("new record inserted");
        }
    }

    private int getTeamIdByNumber(LogStringParsingResult parsingResult) throws Exception {
        int teamNumber = parsingResult.extractTeamNumber();
        Team team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        if (team == null) throw new Exception("team not found by number: " + teamNumber);
        return team.getTeamId();
    }

    private Date substituteForCommonStart(Date recordDateTime, int teamId) {
        Team team = TeamsRegistry.getInstance().getTeamById(teamId);
        LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
        if (levelPoint.isCommonStart()) {
            return levelPoint.getLevelPointMinDateTime();
        } else {
            return recordDateTime;
        }
    }

    private boolean needUpdateExistingRecord(RawLoggerData existingRecord, Date recordDateTime) {
        int distanceId = existingRecord.getTeam().getDistanceId();
        if (scanPoint.getLevelPointByDistance(distanceId).getPointType().isStart()) {
            // start record - use last check
            return existingRecord.getRecordDateTime().before(recordDateTime);
        } else {
            // finish record - use first check
            return existingRecord.getRecordDateTime().after(recordDateTime);
        }
    }
}
