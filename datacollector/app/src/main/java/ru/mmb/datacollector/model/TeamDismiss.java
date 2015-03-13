package ru.mmb.datacollector.model;

import java.util.Date;

public class TeamDismiss
{
	private final int scanPointId;
	private final int teamId;
	private final int teamUserId;
	private final Date recordDateTime;

	private ScanPoint scanPoint = null;
	private Team team = null;
	private User teamUser = null;

	public TeamDismiss(int scanPointId, int teamId, int teamUserId, Date recordDateTime)
	{
		this.scanPointId = scanPointId;
		this.teamId = teamId;
		this.teamUserId = teamUserId;
		this.recordDateTime = recordDateTime;
	}

	public ScanPoint getScanPoint()
	{
		return scanPoint;
	}

	public void setScanPoint(ScanPoint scanPoint)
	{
		this.scanPoint = scanPoint;
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

	public int getScanPointId()
	{
		return scanPointId;
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

	public LevelPoint getLevelPoint()
	{
		return scanPoint.getLevelPointByDistance(team.getDistanceId());
	}
}
