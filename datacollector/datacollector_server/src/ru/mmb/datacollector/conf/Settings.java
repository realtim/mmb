package ru.mmb.datacollector.conf;

public class Settings {
	public static final String CURRENT_RAID_ID = "current.raid.id";
	public static final String MYSQL_CONNECTION_POOL_SIZE = "mysql.connection.pool.size";
	public static final String MYSQL_CONNECTION_STRING = "mysql.connection.string";
	public static final String MYSQL_CONNECTION_USERNAME = "mysql.connection.username";
	public static final String MYSQL_CONNECTION_PASSWORD = "mysql.connection.password";
	public static final String FILE_UPLOAD_TEMP_DIR = "file.upload.temp.dir";
	public static final String TRANSPORT_USER_ID = "transport.user.id";
	public static final String TRANSPORT_USER_PASSWORD = "transport.user.password";

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

	private String fileUploadTempDir = "c:\\tmp";

	private String transportUserId = "";
	private String transportUserPassword = "";

	public String getFileUploadTempDir() {
		return fileUploadTempDir;
	}

	public void setFileUploadTempDir(String fileUploadTempDir) {
		this.fileUploadTempDir = fileUploadTempDir;
	}

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
				+ dbPassword + "\n\tfileUploadTempDir=" + fileUploadTempDir + "\n\ttransportUserId=" + transportUserId
				+ "\n\ttransportUserPassword=" + transportUserPassword;
	}

	public String getTransportUserId() {
		return transportUserId;
	}

	public void setTransportUserId(String transportUserId) {
		this.transportUserId = transportUserId;
	}

	public String getTransportUserPassword() {
		return transportUserPassword;
	}

	public void setTransportUserPassword(String transportUserPassword) {
		this.transportUserPassword = transportUserPassword;
	}
}
