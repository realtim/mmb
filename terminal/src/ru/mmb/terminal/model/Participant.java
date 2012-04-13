package ru.mmb.terminal.model;

import java.io.Serializable;

public class Participant implements Serializable, Comparable<Participant>
{
	private static final long serialVersionUID = 1134876440887563801L;

	private transient Team team = null;
	private int userId;
	private int teamId;
	private String userName;
	private Integer userBirthYear = null;

	public Participant()
	{
	}

	public Participant(int userId, int teamId, String userName)
	{
		this(userId, teamId, userName, null);
	}

	public Participant(int userId, int teamId, String userName, Integer userBirthYear)
	{
		this.userId = userId;
		this.teamId = teamId;
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

	/*public static Participant parse(String participantString)
	{
		if (ParseUtils.isEmpty(participantString)) return null;

		String[] strings = participantString.split("\\|");
		int id = Integer.parseInt(strings[0]);
		int teamId = Integer.parseInt(strings[1]);
		String name = strings[2];
		Integer birthYear = null;
		if (!ParseUtils.isNull(strings[3])) birthYear = Integer.parseInt(strings[3]);

		return new Participant(id, teamId, name, birthYear);
	}*/

	@Override
	public int compareTo(Participant another)
	{
		return getUserName().compareTo(another.getUserName());
	}
}
