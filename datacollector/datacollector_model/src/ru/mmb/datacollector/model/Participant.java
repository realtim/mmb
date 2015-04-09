package ru.mmb.datacollector.model;

import java.io.Serializable;

public class Participant implements Serializable, Comparable<Participant>
{
	private static final long serialVersionUID = 1134876440887563801L;

	private transient Team team = null;
	private int userId;
	private int teamId;
	private String userName;
	private Integer userBirthYear = null;
	private int teamUserId;

	public Participant()
	{
	}

	public Participant(int userId, int teamId, int teamUserId, String userName)
	{
		this(userId, teamId, teamUserId, userName, null);
	}

	public Participant(int userId, int teamId, int teamUserId, String userName, Integer userBirthYear)
	{
		this.userId = userId;
		this.teamId = teamId;
		this.teamUserId = teamUserId;
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

	public int getTeamId()
	{
		return teamId;
	}

	public Team getTeam()
	{
		return team;
	}

	public void setTeam(Team team)
	{
		this.team = team;
	}

	@Override
	public int compareTo(Participant another)
	{
		return getUserName().compareTo(another.getUserName());
	}

	public int getTeamUserId()
	{
		return teamUserId;
	}
}
