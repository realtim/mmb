package ru.mmb.datacollector.model;

import java.util.Date;

public class RawLoggerData {
	private final int userId;
	private final int deviceId;
	private final int loggerId;
	private final int scanPointId;
	private final int teamId;
	private final Date recordDateTime;
	private final Date scannedDateTime;
	private final int changedManual;

	private ScanPoint scanPoint = null;
	private Team team = null;

	/**
	 * Create logger RAW data just from scanner.
	 * 
	 * @param loggerId
	 * @param scanPointId
	 * @param teamId
	 * @param recordDateTime
	 */
	public RawLoggerData(int loggerId, int scanPointId, int teamId, Date recordDateTime, Date scannedDateTime,
			int changedManual) {
		this(0, 0, loggerId, scanPointId, teamId, recordDateTime, scannedDateTime, changedManual);
	}

	public RawLoggerData(int userId, int deviceId, int loggerId, int scanPointId, int teamId, Date recordDateTime,
			Date scannedDateTime, int changedManual) {
		this.userId = userId;
		this.deviceId = deviceId;
		this.loggerId = loggerId;
		this.scanPointId = scanPointId;
		this.teamId = teamId;
		this.recordDateTime = recordDateTime;
		this.scannedDateTime = scannedDateTime;
		this.changedManual = changedManual;
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

	public int getScanPointId() {
		return scanPointId;
	}

	public int getLoggerId() {
		return loggerId;
	}

	public int getTeamId() {
		return teamId;
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

	public Date getScannedDateTime() {
		return scannedDateTime;
	}

	public int getChangedManual() {
		return changedManual;
	}
}
