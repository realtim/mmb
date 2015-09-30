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
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.transport.importer.Importer;
import ru.mmb.datacollector.util.DateUtils;

public class LoggerDataSaver {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");
    private static final SimpleDateFormat prettyFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final LoggerDataProcessor owner;
    private final SQLiteDatabase db;
    private int recordsToSave;

    public LoggerDataSaver(LoggerDataProcessor owner) {
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

    public void saveToDB(LogStringParsingResult parsingResult) {
        ScanPoint scanPoint = ScanPointsRegistry.getInstance().getScanPointByOrder(parsingResult.getScanpointOrderNumber());
        if (scanPoint == null) return;

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
        recordDateTime = alignToLevelPointLimits(scanPoint, recordDateTime, teamId);
        RawLoggerData existingRecord = SQLiteDatabaseAdapter.getConnectedInstance().getExistingLoggerRecord(loggerId, scanpointId, teamId);
        if (existingRecord != null) {
            if (needUpdateExistingRecord(scanPoint, existingRecord, recordDateTime)) {
                String sql = SQLiteDatabaseAdapter.getConnectedInstance().getUpdateExistingLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime, recordDateTime, 0);
                db.execSQL(sql);
                recordsToSave++;
            }
        } else {
            String sql = SQLiteDatabaseAdapter.getConnectedInstance().getInsertNewLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime, recordDateTime, 0);
            db.execSQL(sql);
            recordsToSave++;
            Team team = TeamsRegistry.getInstance().getTeamById(teamId);
            String message = "SAVING new record inserted [scanpoint: " + scanPoint.getScanPointName()
                    + ", team: " + team.getTeamNum() + ", time: " + prettyFormat.format(recordDateTime) + "]";
            owner.writeToConsole(message);
            Log.d("DATA_SAVER", message);
        }
    }

    private int getTeamIdByNumber(LogStringParsingResult parsingResult) throws Exception {
        int teamNumber = parsingResult.extractTeamNumber();
        Team team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        if (team == null) throw new Exception("team not found by number: " + teamNumber);
        return team.getTeamId();
    }

    private Date alignToLevelPointLimits(ScanPoint scanPoint, Date recordDateTime, int teamId) {
        Team team = TeamsRegistry.getInstance().getTeamById(teamId);
        LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
        if (levelPoint.isCommonStart()) {
            return levelPoint.getLevelPointMinDateTime();
        } else if (levelPoint.getPointType().isStart()) {
            if (recordDateTime.after(levelPoint.getLevelPointMaxDateTime())) {
                String message = "PREPROCESS record start time set to max for point [scanpoint: "
                        + scanPoint.getScanPointName() + ", team: " + team.getTeamNum() + ", recordTime: "
                        + prettyFormat.format(recordDateTime) + ", updatedTime: "
                        + prettyFormat.format(levelPoint.getLevelPointMaxDateTime()) + "]";
                owner.writeError(message);
                Log.d("DATA_SAVER", message);
                return levelPoint.getLevelPointMaxDateTime();
            }
        }
        return recordDateTime;
    }

    private boolean needUpdateExistingRecord(ScanPoint scanPoint, RawLoggerData existingRecord, Date recordDateTime) {
        // if record was changed manually, then update not needed
        if (existingRecord.getChangedManual() == 1) return false;

        int distanceId = existingRecord.getTeam().getDistanceId();
        if (scanPoint.getLevelPointByDistance(distanceId).getPointType().isStart()) {
            // start record - use first check
            if (existingRecord.getScannedDateTime().after(recordDateTime)) {
                String message = "SAVING record start time [scanpoint: " + existingRecord.getScanPoint().getScanPointName()
                        + ", team: " + existingRecord.getTeam().getTeamNum() + ", time: "
                        + prettyFormat.format(recordDateTime) + "]";
                owner.writeError(message);
                Log.d("DATA_SAVER", message);
                return true;
            }

        } else {
            // finish record - use last check
            if (existingRecord.getScannedDateTime().before(recordDateTime)) {
                String message = "SAVING record finish time [scanpoint: " + existingRecord.getScanPoint().getScanPointName()
                        + ", team: " + existingRecord.getTeam().getTeamNum() + ", time: "
                        + prettyFormat.format(recordDateTime) + "]";
                owner.writeError(message);
                Log.d("DATA_SAVER", message);
                return true;
            }
        }
        return false;
    }
}
