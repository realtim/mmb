package ru.mmb.datacollector.model;

import java.util.Date;

public class RawTeamLevelDismiss {
	private final int userId;
	private final int deviceId;
	private final int scanPointId;
	private final int teamId;
	private final int teamUserId;
	private final Date recordDateTime;

	private ScanPoint scanPoint = null;
	private Team team = null;
	private User teamUser = null;

	public RawTeamLevelDismiss(int scanPointId, int teamId, int teamUserId, Date recordDateTime) {
		this(0, 0, scanPointId, teamId, teamUserId, recordDateTime);
	}

	public RawTeamLevelDismiss(int userId, int deviceId, int scanPointId, int teamId, int teamUserId,
			Date recordDateTime) {
		this.userId = userId;
		this.deviceId = deviceId;
		this.scanPointId = scanPointId;
		this.teamId = teamId;
		this.teamUserId = teamUserId;
		this.recordDateTime = recordDateTime;
	}

	public ScanPoint getScanPoint() {
		return scanPoint;
	}

	public void setScanPoint(ScanPoint scanPoint) {
		this.scanPoint = scanPoint;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public User getTeamUser() {
		return teamUser;
	}

	public void setTeamUser(User teamUser) {
		this.teamUser = teamUser;
	}

	public int getScanPointId() {
		return scanPointId;
	}

	public int getTeamId() {
		return teamId;
	}

	public int getTeamUserId() {
		return teamUserId;
	}

	public Date getRecordDateTime() {
		return recordDateTime;
	}

	public int getUserId() {
		return userId;
	}

	public int getDeviceId() {
		return deviceId;
	}
}
