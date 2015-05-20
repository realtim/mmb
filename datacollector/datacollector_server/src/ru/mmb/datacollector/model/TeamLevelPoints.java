package ru.mmb.datacollector.model;

import java.util.Date;

import ru.mmb.datacollector.util.DateFormat;

public class TeamLevelPoints {
	private final int teamId;
	private final int userId;
	private final int deviceId;
	private final int scanPointId;
	private final String takenCheckpointNames;
	private final Date checkDateTime;
	private final Date recordDateTime;

	private Team team = null;
	private ScanPoint scanPoint = null;

	public TeamLevelPoints(int teamId, int userId, int deviceId, int scanPointId, String takenCheckpointNames,
			Date checkDateTime, Date recordDateTime) {
		this.teamId = teamId;
		this.userId = userId;
		this.deviceId = deviceId;
		this.scanPointId = scanPointId;
		this.takenCheckpointNames = takenCheckpointNames;
		this.checkDateTime = checkDateTime;
		this.recordDateTime = recordDateTime;
	}

	public int getTeamId() {
		return teamId;
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

	public String getTakenCheckpointNames() {
		return takenCheckpointNames;
	}

	public Date getCheckDateTime() {
		return checkDateTime;
	}

	public Date getRecordDateTime() {
		return recordDateTime;
	}

	public boolean isCheckDateTimeInMinMaxInterval() {
		LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
		return levelPoint.getLevelPointMinDateTime().getTime() <= checkDateTime.getTime()
				&& levelPoint.getLevelPointMaxDateTime().getTime() >= checkDateTime.getTime();
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

	public String buildCheckDateTimeNotInIntervalMessage() {
		LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
		return DateFormat.format(checkDateTime) + " not in interval ["
				+ DateFormat.format(levelPoint.getLevelPointMinDateTime()) + ", "
				+ DateFormat.format(levelPoint.getLevelPointMaxDateTime()) + "]";
	}

	public String buildSuccesMessage() {
		return "[checkDateTime=" + DateFormat.format(checkDateTime) + ", takenCheckpoints=[" + takenCheckpointNames
				+ "]]";
	}
}
