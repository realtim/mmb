package ru.mmb.datacollector.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.User;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.Settings;

public class SQLiteDatabaseAdapter {
    private static final int BACKUP_SAVES_COUNT = 10;
    private static final int BACKUP_MAX_FILES = 20;

    private SQLiteDatabase db;

    private DistancesDB distancesDB;
    private ScanPointsDB scanPointsDB;
    private LevelPointsDB levelPointsDB;
    private LevelPointDiscountsDB levelPointDiscountsDB;
    private TeamsDB teamsDB;
    private MetaTablesDB metaTablesDB;
    private UsersDB usersDB;
    private RawLoggerDataDB rawLoggerDataDB;
    private RawTeamLevelPointsDB rawTeamLevelPointsDB;
    private RawTeamLevelDismissDB rawTeamLevelDismissDB;

    private IDGenerator idGenerator;

    private long localSaveCount = 0;

    private static SQLiteDatabaseAdapter instance = null;

    private SQLiteDatabaseAdapter() {
    }

    public void tryConnectToDB() {
        try {
            db = SQLiteDatabase.openDatabase(Settings.getInstance().getPathToDB(), null, SQLiteDatabase.OPEN_READWRITE);
            // Log.d("SQLiteDatabaseAdapter", "db open " + Settings.getInstance().getPathToDB());
            performTestQuery();
            // Log.d("SQLiteDatabaseAdapter", "db open SUCCESS");
            distancesDB = new DistancesDB(db);
            scanPointsDB = new ScanPointsDB(db);
            levelPointsDB = new LevelPointsDB(db);
            levelPointDiscountsDB = new LevelPointDiscountsDB(db);
            teamsDB = new TeamsDB(db);
            idGenerator = new IDGenerator(db);
            metaTablesDB = new MetaTablesDB(db);
            usersDB = new UsersDB(db);
            rawLoggerDataDB = new RawLoggerDataDB(db);
            rawTeamLevelPointsDB = new RawTeamLevelPointsDB(db);
            rawTeamLevelDismissDB = new RawTeamLevelDismissDB(db);
        } catch (SQLiteException e) {
            if (db != null) {
                db.close();
                db = null;
            }
        }
    }

    private void performTestQuery() {
        if (db == null) return;

        DistancesDB.performTestQuery(db);
    }

    public boolean isConnected() {
        return db != null;
    }

    public void closeConnection() {
        if (isConnected()) {
            db.close();
            db = null;
        }
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public List<Distance> loadDistances(int raidId) {
        return distancesDB.loadDistances(raidId);
    }

    public List<Team> loadTeams() {
        return teamsDB.loadTeams();
    }

    public int getNextId() {
        return idGenerator.getNextId();
    }

    public List<MetaTable> loadMetaTables() {
        return metaTablesDB.loadMetaTables();
    }

    public List<User> loadUsers() {
        return usersDB.loadUsers();
    }

    public List<ScanPoint> loadScanPoints(int raidId) {
        return scanPointsDB.loadScanPoints(raidId);
    }

    public List<LevelPoint> loadLevelPoints(int raidId) {
        return levelPointsDB.loadLevelPoints(raidId);
    }

    public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId) {
        return levelPointDiscountsDB.loadLevelPointDiscounts(raidId);
    }

    public RawTeamLevelPointsRecord getExistingTeamResultRecord(ScanPoint scanPoint, Team team) {
        return rawTeamLevelPointsDB.getExistingTeamResultRecord(scanPoint, team);
    }

    public List<RawTeamLevelPoints> loadRawTeamLevelPoints(ScanPoint scanPoint) {
        return rawTeamLevelPointsDB.loadRawTeamLevelPoints(scanPoint);
    }

    public List<RawTeamLevelPoints> loadAllLevelPointsForDevice() {
        return rawTeamLevelPointsDB.loadAllLevelPointsForDevice();
    }

    public void saveRawTeamLevelPoints(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime) {
        rawTeamLevelPointsDB.saveRawTeamLevelPoints(scanPoint, team, takenCheckpoints, recordDateTime);
    }

    public List<RawTeamLevelDismiss> loadDismissedMembers(ScanPoint scanPoint) {
        return rawTeamLevelDismissDB.loadDismissedMembers(scanPoint);
    }

    public List<RawTeamLevelDismiss> loadAllDismissedMembersForDevice() {
        return rawTeamLevelDismissDB.loadAllDismissedMembersForDevice();
    }

    public List<Participant> getDismissedMembers(ScanPoint scanPoint, Team team) {
        return rawTeamLevelDismissDB.getDismissedMembers(scanPoint, team);
    }

    public void saveDismissedMembers(ScanPoint scanPoint, Team team,
                                     List<Participant> dismissedMembers, Date recordDateTime) {
        rawTeamLevelDismissDB.saveDismissedMembers(scanPoint, team, dismissedMembers, recordDateTime);
    }

    public void appendScanPointTeams(ScanPoint scanPoint, Set<Integer> teams) {
        rawTeamLevelPointsDB.appendScanPointTeams(scanPoint, teams);
        rawTeamLevelDismissDB.appendScanPointTeams(scanPoint, teams);
    }

    public RawLoggerData getExistingLoggerRecord(int loggerId, int scanpointId, int teamId) {
        return rawLoggerDataDB.getExistingRecord(loggerId, scanpointId, teamId);
    }

    public RawLoggerData getExistingLoggerRecord(ScanPoint scanPoint, Team team) {
        return rawLoggerDataDB.getExistingRecord(scanPoint, team);
    }

    public String getUpdateExistingLoggerRecordSql(int loggerId, int scanpointId, int teamId, Date recordDateTime, Date scannedDateTime, int changedManual) {
        return rawLoggerDataDB.getUpdateExistingLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime, scannedDateTime, changedManual);
    }

    public String getInsertNewLoggerRecordSql(int loggerId, int scanpointId, int teamId, Date recordDateTime, Date scannedDateTime, int changedManual) {
        return rawLoggerDataDB.getInsertNewLoggerRecordSql(loggerId, scanpointId, teamId, recordDateTime, scannedDateTime, changedManual);
    }

    public void saveRawLoggerDataManual(ScanPoint scanPoint, Team team, Date scannedDateTime, Date recordDateTime) {
        rawLoggerDataDB.saveRawLoggerDataManual(scanPoint, team, scannedDateTime, recordDateTime);
    }

    public List<RawLoggerData> loadRawLoggerData(ScanPoint scanPoint) {
        return rawLoggerDataDB.loadRawLoggerData(scanPoint);
    }

    public List<RawLoggerData> loadAllRawLoggerDataForDevice() {
        return rawLoggerDataDB.loadAllRawLoggerDataForDevice();
    }

    public static SQLiteDatabaseAdapter getRawInstance() {
        if (instance == null) {
            instance = new SQLiteDatabaseAdapter();
            instance.tryConnectToDB();
        }
        return instance;
    }

    public static SQLiteDatabaseAdapter getConnectedInstance() {
        SQLiteDatabaseAdapter result = getRawInstance();
        if (!result.isConnected())
            return null;
        return result;
    }

    public void backupDatabase(Context context) {
        closeConnection();
        saveDatatbaseToBackupDir(context);
        tryConnectToDB();
    }

    private void saveDatatbaseToBackupDir(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        createBackupDirIfNotExists();
        removeObsoleteBackupFiles();
        try {
            String currentDBPath = Settings.getInstance().getPathToDB();
            String backupDBPath = Settings.getInstance().getDBBackupDir() + "/datacollector_" + sdf.format(new Date()) + ".db";
            File currentDB = new File(currentDBPath);
            File backupDB = new File(backupDBPath);

            FileChannel src = null;
            FileChannel dst = null;
            try {
                src = new FileInputStream(currentDB).getChannel();
                dst = new FileOutputStream(backupDB, false).getChannel();
                dst.transferFrom(src, 0, src.size());
            } finally {
                if (src != null) src.close();
                if (dst != null) dst.close();
            }

            Toast.makeText(context, "datacollector.db backup SUCCESS", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("BACKUP_DB", "backup failed", e);
            Toast.makeText(context, "datacollector.db backup FAILED", Toast.LENGTH_LONG).show();
        }
    }

    private void createBackupDirIfNotExists() {
        File backupDir = new File(Settings.getInstance().getDBBackupDir());
        if (!backupDir.exists()) {
            backupDir.mkdir();
        }
    }

    private void removeObsoleteBackupFiles() {
        File backupDir = new File(Settings.getInstance().getDBBackupDir());
        List<File> files = Arrays.asList(backupDir.listFiles());
        if (files.size() < BACKUP_MAX_FILES) return;
        // get earliest files first
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return new Long(file1.lastModified()).compareTo(new Long(file2.lastModified()));
            }
        });
        // remove first extra files
        int filesToRemoveCount = files.size() - (BACKUP_MAX_FILES - 1);
        for (int i = 0; i < filesToRemoveCount; i++) {
            files.get(i).delete();
        }
    }

    public void incLocalSaveCount() {
        localSaveCount++;
    }

    public void backupDatabaseIfNeeded(Context context) {
        if (localSaveCount % BACKUP_SAVES_COUNT == 0) {
            backupDatabase(context);
        }
    }
}
