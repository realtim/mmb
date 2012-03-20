package ru.mmb.terminal.model;

import java.io.Serializable;
import java.security.MessageDigest;

public class User implements Serializable
{
	private static final long serialVersionUID = -5316566445893527898L;

	private int id;
	private String name;
	private String passwordHash;

	public User()
	{
	}

	public User(int id, String name, String passwordHash)
	{
		this.id = id;
		this.name = name;
		this.passwordHash = passwordHash;
	}

	public int getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getPasswordHash()
	{
		return passwordHash;
	}

	public static User parse(String userString)
	{
		String[] userInfo = userString.split("\\|");
		return new User(Integer.parseInt(userInfo[0]), userInfo[1], userInfo[2]);
	}

	@Override
	public String toString()
	{
		return "User [id=" + id + ", name=" + name + ", passwordHash=" + passwordHash + "]";
	}

	public boolean checkPassword(String userPassword)
	{
		try
		{
			byte[] bytes = userPassword.getBytes("UTF-8");
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] digest = md5.digest(bytes);
			String digestString = "";
			for (byte _byte : digest)
			{
				digestString += getHexString(_byte);
			}
			return digestString.equalsIgnoreCase(getPasswordHash());
		}
		catch (Exception e)
		{
			System.out.println("password check failed " + e.getMessage());
			return false;
		}
	}

	private String getHexString(byte _byte)
	{
		String result = Integer.toHexString(0xff & _byte).toUpperCase();
		if (result.length() == 1) result = "0" + result;
		return result;
	}
}
