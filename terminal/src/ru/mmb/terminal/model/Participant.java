package ru.mmb.terminal.model;

import java.io.Serializable;

import ru.mmb.terminal.model.registry.Parented;

public class Participant implements Serializable, Parented, Comparable<Participant>
{
	private static final long serialVersionUID = 1134876440887563801L;

	private transient Team team = null;
	private int id;
	private int teamId;
	private String name;
	private Integer birthYear = null;
	private boolean withdrawn = false;

	public Participant()
	{
	}

	public Participant(int id, int teamId, String name)
	{
		this(id, teamId, name, null);
	}

	public Participant(int id, int teamId, String name, Integer birthYear)
	{
		this.id = id;
		this.teamId = teamId;
		this.name = name;
		this.birthYear = birthYear;
	}

	public int getId()
	{
		return id;
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

	public String getName()
	{
		return name;
	}

	public Integer getBirthYear()
	{
		return birthYear;
	}

	public static Participant parse(String participantString)
	{
		if (ParseUtils.isEmpty(participantString)) return null;

		String[] strings = participantString.split("\\|");
		int id = Integer.parseInt(strings[0]);
		int teamId = Integer.parseInt(strings[1]);
		String name = strings[2];
		Integer birthYear = null;
		if (!ParseUtils.isNull(strings[3])) birthYear = Integer.parseInt(strings[3]);

		return new Participant(id, teamId, name, birthYear);
	}

	@Override
	public Integer getParentId()
	{
		return new Integer(getTeamId());
	}

	public boolean isWithdrawn()
	{
		return withdrawn;
	}

	public void setWithdrawn(boolean withdrawn)
	{
		this.withdrawn = withdrawn;
	}

	@Override
	public String toString()
	{
		return "Participant [id=" + id + ", teamId=" + teamId + ", name=" + name + ", birthYear="
		        + birthYear + ", withdrawn=" + withdrawn + "]";
	}

	@Override
	public int compareTo(Participant another)
	{
		return getName().compareTo(another.getName());
	}
}
