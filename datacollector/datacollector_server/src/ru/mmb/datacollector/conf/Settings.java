package ru.mmb.datacollector.conf;

public class Settings {
	public static final String CURRENT_RAID_ID = "current.raid.id";
	public static final String MYSQL_CONNECTION_POOL_SIZE = "mysql.connection.pool.size";
	public static final String MYSQL_CONNECTION_STRING = "mysql.connection.string";
	public static final String MYSQL_CONNECTION_USERNAME = "mysql.connection.username";
	public static final String MYSQL_CONNECTION_PASSWORD = "mysql.connection.password";

	private static Settings instance = null;

	private Settings() {
	}

	public synchronized static Settings getInstance() {
		if (instance == null) {
			instance = new Settings();
		}
		return instance;
	}

	private int currentRaidId = -1;

	private int dbPoolSize = 10;
	private String dbConnectionString = "jdbc:mysql://localhost:3306/datacollector";
	private String dbUserName = "datacollector";
	private String dbPassword = "datacollector";

	public int getCurrentRaidId() {
		return currentRaidId;
	}

	public void setCurrentRaidId(int currentRaidId) {
		this.currentRaidId = currentRaidId;
	}

	public int getDbPoolSize() {
		return dbPoolSize;
	}

	public void setDbPoolSize(int dbPoolSize) {
		this.dbPoolSize = dbPoolSize;
	}

	public String getDbConnectionString() {
		return dbConnectionString;
	}

	public void setDbConnectionString(String dbConnectionString) {
		this.dbConnectionString = dbConnectionString;
	}

	public String getDbUserName() {
		return dbUserName;
	}

	public void setDbUserName(String dbUserName) {
		this.dbUserName = dbUserName;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	@Override
	public String toString() {
		return "\nSettings:\n\tcurrentRaidId=" + currentRaidId + "\n\tdbPoolSize=" + dbPoolSize
				+ "\n\tdbConnectionString=" + dbConnectionString + "\n\tdbUserName=" + dbUserName + "\n\tdbPassword="
				+ dbPassword;
	}
}
