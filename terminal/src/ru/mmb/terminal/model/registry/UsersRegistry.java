package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.model.User;
import ru.mmb.terminal.util.ExternalStorage;

public class UsersRegistry extends AbstractRegistry
{
	private static UsersRegistry instance = null;

	private List<User> users = null;

	public static UsersRegistry getInstance()
	{
		if (instance == null)
		{
			instance = new UsersRegistry();
		}
		return instance;
	}

	private UsersRegistry()
	{
		load();
	}

	private void load()
	{
		try
		{
			users = loadUsers();
		}
		catch (Exception e)
		{
			users = new ArrayList<User>();
		}
	}

	private List<User> loadUsers() throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/users.csv", User.class);
	}

	public String[] getUserNamesArray()
	{
		String[] result = new String[users.size()];
		for (int i = 0; i < users.size(); i++)
		{
			result[i] = users.get(i).getName();
		}
		return result;
	}

	public boolean checkUserPassword(String userName, String userPassword)
	{
		if (userName == null || userName.length() == 0) return false;
		User user = findUser(userName);
		if (user == null) return false;
		return user.checkPassword(userPassword);
	}

	public User findUser(String userName)
	{
		for (User user : users)
		{
			if (user.getName().equalsIgnoreCase(userName)) return user;
		}
		return null;
	}

	public int findIndexByName(String userName)
	{
		for (int i = 0; i < users.size(); i++)
		{
			if (users.get(i).getName().equalsIgnoreCase(userName)) return i;
		}
		return -1;
	}

	public User getUserById(int id)
	{
		for (User user : users)
		{
			if (user.getId() == id) return user;
		}
		return null;
	}
}
