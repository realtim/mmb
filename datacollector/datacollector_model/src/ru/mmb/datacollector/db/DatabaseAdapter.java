package ru.mmb.datacollector.db;

import java.util.List;

import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.User;

public abstract class DatabaseAdapter {
	public static DatabaseAdapterFactory databaseAdapterFactory = null;

	private static DatabaseAdapter instance = null;

	public static synchronized DatabaseAdapter getRawInstance() {
		if (instance == null) {
			instance = databaseAdapterFactory.createDatabaseAdapter();
			instance.tryConnectToDB();
		}
		return instance;
	}

	public static DatabaseAdapter getConnectedInstance() {
		DatabaseAdapter result = getRawInstance();
		if (!result.isConnected())
			return null;
		return result;
	}

	public abstract void tryConnectToDB();

	public abstract boolean isConnected();

	public abstract List<Distance> loadDistances(int raidId);

	public abstract List<ScanPoint> loadScanPoints(int raidId);

	public abstract List<LevelPoint> loadLevelPoints(int raidId);

	public abstract List<LevelPointDiscount> loadLevelPointDiscounts(int raidId);

	public abstract List<Team> loadTeams();

	public abstract List<User> loadUsers();
}
