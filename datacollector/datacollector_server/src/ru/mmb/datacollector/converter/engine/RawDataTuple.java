package ru.mmb.datacollector.converter.engine;

import java.util.Date;

import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamLevelPoints;

public class RawDataTuple {
	private RawLoggerData rawLoggerData = null;
	private RawTeamLevelPoints rawTeamLevelPoints = null;

	public void setRawLoggerData(RawLoggerData rawLoggerData) {
		if (this.rawLoggerData != null) {
			if (ignoreNewRawLoggerData(this.rawLoggerData, rawLoggerData)) {
				return;
			}
		}
		this.rawLoggerData = rawLoggerData;
	}

	private boolean ignoreNewRawLoggerData(RawLoggerData oldRecord, RawLoggerData newRecord) {
		int distanceId = oldRecord.getTeam().getDistanceId();
		if (oldRecord.getScanPoint().getLevelPointByDistance(distanceId).getPointType().isStart()) {
			// start record - ignore earlier record
			return oldRecord.getRecordDateTime().after(newRecord.getRecordDateTime());
		} else {
			// finish record - ignore later record
			return oldRecord.getRecordDateTime().before(newRecord.getRecordDateTime());
		}
	}

	public void setRawTeamLevelPoints(RawTeamLevelPoints rawTeamLevelPoints) {
		if (this.rawTeamLevelPoints != null) {
			if (this.rawTeamLevelPoints.getRecordDateTime().after(rawTeamLevelPoints.getRecordDateTime())) {
				return;
			}
		}
		this.rawTeamLevelPoints = rawTeamLevelPoints;
	}

	public boolean isFull() {
		if (rawLoggerData == null)
			return false;
		int distanceId = rawLoggerData.getTeam().getDistanceId();
		if (rawLoggerData.getScanPoint().getLevelPointByDistance(distanceId).getPointType().isStart()) {
			// on start no checkpoints record is needed
			return true;
		} else {
			return rawTeamLevelPoints != null;
		}
	}

	public String buildNotFullMessage() {
		String message = "";
		if (rawLoggerData == null) {
			message += "rawLoggerData not found";
		}
		if (rawTeamLevelPoints == null) {
			message += "rawTeamLevelPoints not found";
		}
		return message;
	}

	public TeamLevelPoints combineData(Team team) {
		int teamId = team.getTeamId();
		int userId = rawTeamLevelPoints != null ? rawTeamLevelPoints.getUserId() : rawLoggerData.getUserId();
		int deviceId = rawTeamLevelPoints != null ? rawTeamLevelPoints.getDeviceId() : rawLoggerData.getDeviceId();
		int scanPointId = rawLoggerData.getScanPointId();
		String takenCheckpointNames = rawTeamLevelPoints != null ? rawTeamLevelPoints.getTakenCheckpointNames() : "";
		Date checkedDateTime = rawLoggerData.getRecordDateTime();
		Date recordDateTime = rawTeamLevelPoints != null ? rawTeamLevelPoints.getRecordDateTime() : rawLoggerData
				.getRecordDateTime();
		TeamLevelPoints result = new TeamLevelPoints(teamId, userId, deviceId, scanPointId, takenCheckpointNames,
				checkedDateTime, recordDateTime);
		// init reference fields
		result.setScanPoint(rawLoggerData.getScanPoint());
		result.setTeam(team);
		return result;
	}
}
