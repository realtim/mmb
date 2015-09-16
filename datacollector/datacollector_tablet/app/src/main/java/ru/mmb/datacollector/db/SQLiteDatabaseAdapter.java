package ru.mmb.datacollector.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

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
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.User;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.Settings;

public class SQLiteDatabaseAdapter extends DatabaseAdapter {
    private SQLiteDatabase db;

    private DistancesDB distancesDB;
    private ScanPointsDB scanPointsDB;
    private LevelPointsDB levelPointsDB;
    private LevelPointDiscountsDB levelPointDiscountsDB;
    private TeamsDB teamsDB;
    private MetaTablesDB metaTablesDB;
    private UsersDB usersDB;
    private TeamResultsDB teamResultsDB;
    private RawLoggerDataDB rawLoggerDataDB;
    private RawTeamLevelPointsDB rawTeamLevelPointsDB;
    private RawTeamLevelDismissDB rawTeamLevelDismissDB;

    private IDGenerator idGenerator;

    private SQLiteDatabaseAdapter() {
    }

    @Override
    public void tryConnectToDB() {
        try {
            db =
                    SQLiteDatabase.openDatabase(Settings.getInstance().getPathToDB(), null, SQLiteDatabase.OPEN_READWRITE);
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
            teamResultsDB = new TeamResultsDB(db);
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

    @Override
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

    @Override
    public List<Distance> loadDistances(int raidId) {
        return distancesDB.loadDistances(raidId);
    }

    @Override
    public List<Team> loadTeams() {
        return teamsDB.loadTeams();
    }

    public int getNextId() {
        return idGenerator.getNextId();
    }

    public List<MetaTable> loadMetaTables() {
        return metaTablesDB.loadMetaTables();
    }

    @Override
    public List<User> loadUsers() {
        return usersDB.loadUsers();
    }

    @Override
    public List<ScanPoint> loadScanPoints(int raidId) {
        return scanPointsDB.loadScanPoints(raidId);
    }

    @Override
    public List<LevelPoint> loadLevelPoints(int raidId) {
        return levelPointsDB.loadLevelPoints(raidId);
    }

    @Override
    public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId) {
        return levelPointDiscountsDB.loadLevelPointDiscounts(raidId);
    }

    public RawTeamLevelPointsRecord getExistingTeamResultRecord(ScanPoint scanPoint, Team team) {
        return rawTeamLevelPointsDB.getExistingTeamResultRecord(scanPoint, team);
    }

    public List<RawTeamLevelPoints> loadRawTeamLevelPoints(ScanPoint scanPoint) {
        return rawTeamLevelPointsDB.loadRawTeamLevelPoints(scanPoint);
    }

    public void saveRawTeamLevelPoints(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime) {
        rawTeamLevelPointsDB.saveRawTeamLevelPoints(scanPoint, team, takenCheckpoints, recordDateTime);
    }

    public List<RawTeamLevelDismiss> loadDismissedMembers(ScanPoint scanPoint) {
        return rawTeamLevelDismissDB.loadDismissedMembers(scanPoint);
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

    public List<TeamResult> loadTeamResults(Team team) {
        return teamResultsDB.loadTeamResults(team);
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

    public static void init() {
        DatabaseAdapter.databaseAdapterFactory = new SQLiteDatatbaseAdapterFactory();
        Log.d("DATABASE_ADAPTER", "initialized");
    }

    public static SQLiteDatabaseAdapter getRawInstance() {
        return (SQLiteDatabaseAdapter) DatabaseAdapter.getRawInstance();
    }

    public static SQLiteDatabaseAdapter getConnectedInstance() {
        return (SQLiteDatabaseAdapter) DatabaseAdapter.getConnectedInstance();
    }

    private static class SQLiteDatatbaseAdapterFactory implements DatabaseAdapterFactory {
        @Override
        public DatabaseAdapter createDatabaseAdapter() {
            return new SQLiteDatabaseAdapter();
        }
    }
}
