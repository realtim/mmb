package ru.mmb.terminal.model;

import java.util.Date;

public class BarCodeScan implements Comparable<BarCodeScan>
{
	private final int teamId;
	private final int deviceId;
	private final int levelPointId;
	private final Date checkDateTime;
	private final Date recordDateTime;

	private Team team = null;
	private LevelPoint levelPoint = null;

	public BarCodeScan(int teamId, int deviceId, int levelPointId, Date checkDateTime, Date recordDateTime)
	{
		this.teamId = teamId;
		this.deviceId = deviceId;
		this.levelPointId = levelPointId;
		this.checkDateTime = checkDateTime;
		this.recordDateTime = recordDateTime;
	}

	public Team getTeam()
	{
		return team;
	}

	public void setTeam(Team team)
	{
		this.team = team;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	public void setLevelPoint(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
	}

	public int getTeamId()
	{
		return teamId;
	}

	public int getDeviceId()
	{
		return deviceId;
	}

	public int getLevelPointId()
	{
		return levelPointId;
	}

	public Date getCheckDateTime()
	{
		return checkDateTime;
	}

	public Date getRecordDateTime()
	{
		return recordDateTime;
	}

	@Override
	public String toString()
	{
		return "BarCodeScan [teamId=" + teamId + ", deviceId=" + deviceId + ", levelPointId="
		        + levelPointId + ", checkDateTime=" + checkDateTime + ", recordDateTime="
		        + recordDateTime + "]";
	}

	@Override
	public int compareTo(BarCodeScan another)
	{
		int result = this.checkDateTime.compareTo(another.checkDateTime);
		if (result == 0)
		{
			result = (new Integer(teamId)).compareTo(new Integer(another.teamId));
		}
		return result;
	}
}
