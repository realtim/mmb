package ru.mmb.datacollector.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.model.User;

public class UsersDB {
	private static final Logger logger = LogManager.getLogger(UsersDB.class);

	private static final String TABLE_USERS = "Users";

	private static final String USER_ID = "user_id";
	private static final String USER_NAME = "user_name";
	private static final String USER_BIRTHYEAR = "user_birthyear";

	public static synchronized List<User> loadUsers() {
		List<User> result = new ArrayList<User>();
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				String sql = "select `" + USER_ID + "`, `" + USER_NAME + "`, `" + USER_BIRTHYEAR + "` from `"
						+ TABLE_USERS + "`";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					int userId = rs.getInt(1);
					String userName = rs.getString(2);
					Integer userBirthYear = rs.getInt(3);
					if (rs.wasNull()) {
						userBirthYear = null;
					}
					result.add(new User(userId, userName, userBirthYear));
				}
			} finally {
				try {
					if (rs != null) {
						rs.close();
						rs = null;
					}
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}

		return result;
	}
}
