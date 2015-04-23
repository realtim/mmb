package ru.mmb.datacollector.model;

import java.util.Date;

import ru.mmb.datacollector.model.registry.UsersRegistry;

public class TeamLevelDismiss {
	private final int userId;
	private final int deviceId;
	private final int scanPointId;
	private final int teamId;
	private final int teamUserId;
	private final Date recordDateTime;

	private Team team = null;
	private ScanPoint scanPoint = null;

	public TeamLevelDismiss(int userId, int deviceId, int scanPointId, int teamId, int teamUserId, Date recordDateTime) {
		this.userId = userId;
		this.deviceId = deviceId;
		this.scanPointId = scanPointId;
		this.teamId = teamId;
		this.teamUserId = teamUserId;
		this.recordDateTime = recordDateTime;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

	public ScanPoint getScanPoint() {
		return scanPoint;
	}

	public void setScanPoint(ScanPoint scanPoint) {
		this.scanPoint = scanPoint;
	}

	public int getUserId() {
		return userId;
	}

	public int getDeviceId() {
		return deviceId;
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

	public String buildSuccesMessage() {
		User teamUser = UsersRegistry.getInstance().getUserById(teamUserId);
		return "[teamUser=[" + teamUser.getUserName() + "]]";
	}

	public String buildRecordDateTimeEarlierMessage() {
		User teamUser = UsersRegistry.getInstance().getUserById(teamUserId);
		return "[creatorUserId=" + userId + ", teamUser=[" + teamUser.getUserName() + "]]";
	}

	public boolean isRecordDateTimeEarlier(TeamLevelDismiss other) {
		return this.getRecordDateTime().before(other.getRecordDateTime());
	}
}
