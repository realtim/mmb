package ru.mmb.terminal.model;

import java.util.Date;

public class TeamLevelDismiss
{
	private final int levelPointId;
	private final int teamId;
	private final int teamUserId;
	private final Date recordDateTime;

	private LevelPoint levelPoint = null;
	private Team team = null;
	private User teamUser = null;

	public TeamLevelDismiss(int levelPointId, int teamId, int teamUserId, Date recordDateTime)
	{
		this.levelPointId = levelPointId;
		this.teamId = teamId;
		this.teamUserId = teamUserId;
		this.recordDateTime = recordDateTime;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	public void setLevelPoint(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
	}

	public Team getTeam()
	{
		return team;
	}

	public void setTeam(Team team)
	{
		this.team = team;
	}

	public User getTeamUser()
	{
		return teamUser;
	}

	public void setTeamUser(User teamUser)
	{
		this.teamUser = teamUser;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public int getTeamId()
	{
		return teamId;
	}

	public int getTeamUserId()
	{
		return teamUserId;
	}

	public Date getRecordDateTime()
	{
		return recordDateTime;
	}
}
