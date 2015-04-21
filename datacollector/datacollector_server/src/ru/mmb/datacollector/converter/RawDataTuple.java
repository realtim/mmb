package ru.mmb.datacollector.converter;

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
		return rawLoggerData != null && rawTeamLevelPoints != null;
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
		TeamLevelPoints result = new TeamLevelPoints(team.getTeamId(), rawTeamLevelPoints.getUserId(),
				rawTeamLevelPoints.getDeviceId(), rawLoggerData.getScanPointId(),
				rawTeamLevelPoints.getTakenCheckpointNames(), rawLoggerData.getRecordDateTime(),
				rawTeamLevelPoints.getRecordDateTime());
		result.setScanPoint(rawLoggerData.getScanPoint());
		result.setTeam(team);
		return result;
	}
}
