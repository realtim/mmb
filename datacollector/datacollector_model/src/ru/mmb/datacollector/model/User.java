package ru.mmb.datacollector.model;

public class User implements Comparable<User>
{
	private final int userId;
	private final String userName;
	private Integer userBirthYear = null;

	public User(int userId, String userName)
	{
		this(userId, userName, null);
	}

	public User(int userId, String userName, Integer userBirthYear)
	{
		this.userId = userId;
		this.userName = userName;
		this.userBirthYear = userBirthYear;
	}

	public int getUserId()
	{
		return userId;
	}

	public String getUserName()
	{
		return userName;
	}

	public Integer getUserBirthYear()
	{
		return userBirthYear;
	}

	@Override
	public int compareTo(User another)
	{
		return userName.compareTo(another.userName);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + userId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		User other = (User) obj;
		if (userId != other.userId) return false;
		return true;
	}
}
