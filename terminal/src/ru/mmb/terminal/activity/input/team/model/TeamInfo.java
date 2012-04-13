package ru.mmb.terminal.activity.input.team.model;

import ru.mmb.terminal.model.Team;

public class TeamInfo
{
	private final int teamId;
	private final int teamNumber;
	private final String teamName;

	public TeamInfo(Team team)
	{
		this.teamId = team.getTeamId();
		this.teamNumber = team.getTeamNum();
		this.teamName = team.getTeamName();
	}

	public int getTeamId()
	{
		return teamId;
	}

	public int getTeamNumber()
	{
		return teamNumber;
	}

	public String getTeamName()
	{
		return teamName;
	}
}
