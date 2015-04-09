package ru.mmb.datacollector.activity.report.team.search.model;

import ru.mmb.datacollector.model.Team;

public class TeamInfo
{
	private final Team team;

	public TeamInfo(Team team)
	{
		this.team = team;
	}

	public int getTeamId()
	{
		return team.getTeamId();
	}

	public int getTeamNumber()
	{
		return team.getTeamNum();
	}

	public String getTeamName()
	{
		return team.getTeamName();
	}

	public Team getTeam()
	{
		return team;
	}
}
