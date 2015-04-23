package ru.mmb.datacollector.converter.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.converter.DataConverterThread;
import ru.mmb.datacollector.db.ConnectionPool;
import ru.mmb.datacollector.db.MysqlDatabaseAdapter;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamLevelDismiss;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.transport.importer.Importer;

public class TeamLevelDismissEngine extends AbstractConvertationEngine {
	private static final Logger logger = LogManager.getLogger(TeamLevelDismissEngine.class);

	private ScanPoint currentScanPoint;

	public TeamLevelDismissEngine(DataConverterThread owner) {
		super(owner);
	}

	public void convertTeamLevelDismiss() {
		logger.info("TeamLevelDismiss convertation started");
		List<ScanPoint> scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
		for (ScanPoint scanPoint : scanPoints) {
			if (isTerminated()) {
				return;
			}
			currentScanPoint = scanPoint;
			processTeamLevelDismissForScanPoint();
		}
		logger.info("TeamLevelDismiss convertation finished");
	}

	private void processTeamLevelDismissForScanPoint() {
		List<RawTeamLevelDismiss> rawTeamLevelDismissList = MysqlDatabaseAdapter.getConnectedInstance()
				.loadRawTeamLevelDismiss(currentScanPoint.getScanPointId());
		List<TeamLevelDismiss> convertedRecords = convertToTeamLevelDismiss(rawTeamLevelDismissList);
		List<TeamLevelDismiss> recordsToSave = removeDuplicates(convertedRecords);
		saveTeamLevelDismissRecords(recordsToSave);
	}

	private List<TeamLevelDismiss> convertToTeamLevelDismiss(List<RawTeamLevelDismiss> sourceList) {
		List<TeamLevelDismiss> result = new ArrayList<TeamLevelDismiss>();
		for (RawTeamLevelDismiss rawTeamLevelDismiss : sourceList) {
			TeamLevelDismiss teamLevelDismiss = copyTeamLevelDismiss(rawTeamLevelDismiss);
			result.add(teamLevelDismiss);
		}
		return result;
	}

	private TeamLevelDismiss copyTeamLevelDismiss(RawTeamLevelDismiss rawTeamLevelDismiss) {
		TeamLevelDismiss teamLevelDismiss = new TeamLevelDismiss(rawTeamLevelDismiss.getUserId(),
				rawTeamLevelDismiss.getDeviceId(), rawTeamLevelDismiss.getScanPointId(),
				rawTeamLevelDismiss.getTeamId(), rawTeamLevelDismiss.getTeamUserId(),
				rawTeamLevelDismiss.getRecordDateTime());
		teamLevelDismiss.setTeam(rawTeamLevelDismiss.getTeam());
		teamLevelDismiss.setScanPoint(rawTeamLevelDismiss.getScanPoint());
		return teamLevelDismiss;
	}

	private List<TeamLevelDismiss> removeDuplicates(List<TeamLevelDismiss> teamLevelDismissList) {
		Map<RawDismissKey, TeamLevelDismiss> uniqueRecords = new HashMap<RawDismissKey, TeamLevelDismiss>();
		for (TeamLevelDismiss currentRecord : teamLevelDismissList) {
			RawDismissKey currentRecordKey = new RawDismissKey(currentRecord.getTeamId(), currentRecord.getTeamUserId());
			if (uniqueRecords.containsKey(currentRecordKey)) {
				TeamLevelDismiss existingRecord = uniqueRecords.get(currentRecordKey);
				String message = buildErrorPrefix(currentRecord.getTeam());
				if (existingRecord.isRecordDateTimeEarlier(currentRecord)) {
					uniqueRecords.put(currentRecordKey, currentRecord);
					message += existingRecord.buildRecordDateTimeEarlierMessage();
				} else {
					message += currentRecord.buildRecordDateTimeEarlierMessage();
				}
				logger.error(message);
			} else {
				uniqueRecords.put(currentRecordKey, currentRecord);
			}
		}
		return copyUniqueRecordsToList(uniqueRecords);
	}

	private List<TeamLevelDismiss> copyUniqueRecordsToList(Map<RawDismissKey, TeamLevelDismiss> uniqueRecords) {
		List<TeamLevelDismiss> result = new ArrayList<TeamLevelDismiss>();
		for (TeamLevelDismiss teamLevelDismiss : uniqueRecords.values()) {
			String message = buildSuccessPrefix(teamLevelDismiss.getTeam()) + teamLevelDismiss.buildSuccesMessage();
			logger.info(message);
			result.add(teamLevelDismiss);
		}
		return result;
	}

	private String buildSuccessPrefix(Team team) {
		return "SUCCESS building TeamLevelDismiss " + buildPrefix(currentScanPoint, team) + "user dismissed ";
	}

	private String buildErrorPrefix(Team team) {
		return "CHECK FAILED duplicate TeamLevelDismiss " + buildPrefix(currentScanPoint, team) + "user dismissed ";
	}

	private void saveTeamLevelDismissRecords(List<TeamLevelDismiss> recordsToSave) {
		try {
			conn = ConnectionPool.getInstance().getConnection();
			batchStatement = conn.createStatement();
			int recordsInserted = 0;
			for (TeamLevelDismiss teamLevelDismiss : recordsToSave) {
				if (isTerminated()) {
					return;
				}
				String sql = MysqlDatabaseAdapter.getConnectedInstance().getTeamLevelDismissInsertSql(teamLevelDismiss);
				logger.debug(sql);
				batchStatement.addBatch(sql);
				recordsInserted++;
				if (recordsInserted % Importer.ROWS_IN_BATCH == 0) {
					commitBatch();
				}
			}
			commitBatch();
		} catch (Exception e) {
			logger.error("error saving data to TeamLevelDismiss: " + e.getMessage());
			logger.debug("error trace: ", e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
					conn = null;
				}
				if (batchStatement != null) {
					batchStatement.close();
					batchStatement = null;
				}
			} catch (SQLException e) {
				logger.trace("resources release failed", e);
			}
		}
	}
}
