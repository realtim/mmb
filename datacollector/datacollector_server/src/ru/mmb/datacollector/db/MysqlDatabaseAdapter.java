package ru.mmb.datacollector.db;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamLevelDismiss;
import ru.mmb.datacollector.model.TeamLevelPoints;
import ru.mmb.datacollector.model.User;
import ru.mmb.datacollector.model.meta.MetaTable;

public class MysqlDatabaseAdapter extends DatabaseAdapter {
	private static final Logger logger = LogManager.getLogger(MysqlDatabaseAdapter.class);

	private MysqlDatabaseAdapter() {
	}

	public static void init() {
		DatabaseAdapter.databaseAdapterFactory = new MysqlDatatbaseAdapterFactory();
		logger.info("database adapter initialized");
	}

	public static MysqlDatabaseAdapter getConnectedInstance() {
		return (MysqlDatabaseAdapter) DatabaseAdapter.getConnectedInstance();
	}

	private static class MysqlDatatbaseAdapterFactory implements DatabaseAdapterFactory {
		@Override
		public DatabaseAdapter createDatabaseAdapter() {
			return new MysqlDatabaseAdapter();
		}
	}

	@Override
	public void tryConnectToDB() {
		logger.debug("Connection establishment not needed.");
	}

	@Override
	public boolean isConnected() {
		boolean result = false;
		try {
			result = DistancesDB.checkConnectionAlive();
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	@Override
	public List<Distance> loadDistances(int raidId) {
		return DistancesDB.loadDistances(raidId);
	}

	@Override
	public List<ScanPoint> loadScanPoints(int raidId) {
		return ScanPointsDB.loadScanPoints(raidId);
	}

	@Override
	public List<LevelPoint> loadLevelPoints(int raidId) {
		return LevelPointsDB.loadLevelPoints(raidId);
	}

	@Override
	public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId) {
		return LevelPointDiscountsDB.loadLevelPointDiscounts(raidId);
	}

	@Override
	public List<Team> loadTeams() {
		return TeamsDB.loadTeams();
	}

	@Override
	public List<User> loadUsers() {
		return UsersDB.loadUsers();
	}

	public List<MetaTable> loadMetaTables() {
		return MetaTablesDB.loadMetaTables();
	}

	public List<RawLoggerData> loadRawLoggerData(int scanPointId) {
		return RawLoggerDataDB.loadRawLoggerData(scanPointId);
	}

	public List<RawTeamLevelPoints> loadRawTeamLevelPoints(int scanPointId) {
		return RawTeamLevelPointsDB.loadRawTeamLevelPoints(scanPointId);
	}

	public List<RawTeamLevelDismiss> loadRawTeamLevelDismiss(int scanPointId) {
		return RawTeamLevelDismissDB.loadRawTeamLevelDismiss(scanPointId);
	}

	public String getTeamLevelPointsInsertSql(TeamLevelPoints teamLevelPoints) {
		return TeamLevelPointsDB.getTeamLevelPointsInsertSql(teamLevelPoints);
	}

	public String getTeamLevelDismissInsertSql(TeamLevelDismiss teamLevelDismiss) {
		return TeamLevelDismissDB.getTeamLevelDismissInsertSql(teamLevelDismiss);
	}
}
