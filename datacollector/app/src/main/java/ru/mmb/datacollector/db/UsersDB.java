package ru.mmb.datacollector.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.model.User;

public class UsersDB
{
	private static final String TABLE_USERS = "Users";

	private static final String USER_ID = "user_id";
	private static final String USER_NAME = "user_name";
	private static final String USER_BIRTHYEAR = "user_birthyear";

	private final SQLiteDatabase db;

	public UsersDB(SQLiteDatabase db)
	{
		this.db = db;
	}

	public List<User> loadUsers()
	{
		List<User> result = new ArrayList<User>();
		String sql =
		    "select " + USER_ID + ", " + USER_NAME + ", " + USER_BIRTHYEAR + " from " + TABLE_USERS;
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			int userId = resultCursor.getInt(0);
			String userName = resultCursor.getString(1);
			Integer userBirthYear = null;
			if (!resultCursor.isNull(2)) userBirthYear = resultCursor.getInt(2);
			result.add(new User(userId, userName, userBirthYear));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}
}
