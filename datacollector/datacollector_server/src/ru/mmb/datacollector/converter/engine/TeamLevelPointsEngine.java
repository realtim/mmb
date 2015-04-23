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
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamLevelPoints;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.transport.importer.Importer;

public class TeamLevelPointsEngine extends AbstractConvertationEngine {
	private static final Logger logger = LogManager.getLogger(TeamLevelPointsEngine.class);

	private ScanPoint currentScanPoint;

	public TeamLevelPointsEngine(DataConverterThread owner) {
		super(owner);
	}

	public void convertTeamLevelPoints() {
		logger.info("TeamLevelPoints convertation started");
		List<ScanPoint> scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
		for (ScanPoint scanPoint : scanPoints) {
			if (isTerminated()) {
				return;
			}
			currentScanPoint = scanPoint;
			processTeamLevelPointsForScanPoint();
		}
		logger.info("TeamLevelPoints convertation finished");
	}

	private void processTeamLevelPointsForScanPoint() {
		List<RawLoggerData> rawLoggerDataList = MysqlDatabaseAdapter.getConnectedInstance().loadRawLoggerData(
				currentScanPoint.getScanPointId());
		List<RawTeamLevelPoints> rawTeamLevelPointsList = MysqlDatabaseAdapter.getConnectedInstance()
				.loadRawTeamLevelPoints(currentScanPoint.getScanPointId());
		Map<Team, RawDataTuple> joinedRecords = buildJoinedRawDataRecords(rawLoggerDataList, rawTeamLevelPointsList);
		List<TeamLevelPoints> recordsToSave = prepareRecordsToSave(joinedRecords);
		saveTeamLevelPointsRecords(recordsToSave);
	}

	private Map<Team, RawDataTuple> buildJoinedRawDataRecords(List<RawLoggerData> rawLoggerDataList,
			List<RawTeamLevelPoints> rawTeamLevelPointsList) {
		Map<Team, RawDataTuple> result = new HashMap<Team, RawDataTuple>();
		for (RawLoggerData rawLoggerData : rawLoggerDataList) {
			Team team = rawLoggerData.getTeam();
			if (!result.containsKey(team)) {
				result.put(team, new RawDataTuple());
			}
			result.get(team).setRawLoggerData(rawLoggerData);
		}
		for (RawTeamLevelPoints rawTeamLevelPoints : rawTeamLevelPointsList) {
			Team team = rawTeamLevelPoints.getTeam();
			if (!result.containsKey(team)) {
				result.put(team, new RawDataTuple());
			}
			result.get(team).setRawTeamLevelPoints(rawTeamLevelPoints);
		}
		return result;
	}

	private List<TeamLevelPoints> prepareRecordsToSave(Map<Team, RawDataTuple> joinedRecords) {
		List<TeamLevelPoints> result = new ArrayList<TeamLevelPoints>();
		for (Map.Entry<Team, RawDataTuple> entry : joinedRecords.entrySet()) {
			RawDataTuple rawDataTuple = entry.getValue();
			Team team = entry.getKey();
			if (!rawDataTuple.isFull()) {
				String message = buildErrorPrefix(team) + rawDataTuple.buildNotFullMessage();
				logger.error(message);
				continue;
			}
			TeamLevelPoints teamLevelPoints = rawDataTuple.combineData(team);
			if (!teamLevelPoints.isCheckDateTimeInMinMaxInterval()) {
				String message = buildErrorPrefix(team) + teamLevelPoints.buildCheckDateTimeNotInIntervalMessage();
				logger.error(message);
				continue;
			}
			String message = buildSuccessPrefix(team) + teamLevelPoints.buildSuccesMessage();
			logger.info(message);
			result.add(teamLevelPoints);
		}
		return result;
	}

	private String buildSuccessPrefix(Team team) {
		return "SUCCESS building TeamLevelPoints " + buildPrefix(currentScanPoint, team);
	}

	private String buildErrorPrefix(Team team) {
		return "ERROR building TeamLevelPoints " + buildPrefix(currentScanPoint, team);
	}

	private void saveTeamLevelPointsRecords(List<TeamLevelPoints> recordsToSave) {
		try {
			conn = ConnectionPool.getInstance().getConnection();
			batchStatement = conn.createStatement();
			int recordsInserted = 0;
			for (TeamLevelPoints teamLevelPoints : recordsToSave) {
				if (isTerminated()) {
					return;
				}
				String sql = MysqlDatabaseAdapter.getConnectedInstance().getTeamLevelPointsInsertSql(teamLevelPoints);
				logger.debug(sql);
				batchStatement.addBatch(sql);
				recordsInserted++;
				if (recordsInserted % Importer.ROWS_IN_BATCH == 0) {
					commitBatch();
				}
			}
			commitBatch();
		} catch (Exception e) {
			logger.error("error saving data to TeamLevelPoints: " + e.getMessage());
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
