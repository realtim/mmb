package ru.mmb.datacollector.model;

import java.util.Date;

public class RawLoggerData
{
    private final int loggerId;
	private final int scanPointId;
	private final int teamId;
	private final Date recordDateTime;

	private ScanPoint scanPoint = null;
	private Team team = null;

	public RawLoggerData(int loggerId, int scanPointId, int teamId, Date recordDateTime)
	{
        this.loggerId = loggerId;
        this.scanPointId = scanPointId;
		this.teamId = teamId;
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

	public int getScanPointId()
	{
		return scanPointId;
	}

    public int getLoggerId() {
        return loggerId;
    }

	public int getTeamId()
	{
		return teamId;
	}

	public Date getRecordDateTime()
	{
		return recordDateTime;
	}
}
