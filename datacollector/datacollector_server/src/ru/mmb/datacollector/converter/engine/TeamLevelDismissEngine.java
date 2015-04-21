package ru.mmb.datacollector.converter.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

	public TeamLevelDismissEngine(DataConverterThread owner) {
		super(owner);
	}

	public void convertTeamLevelDismiss() {
		List<ScanPoint> scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
		for (ScanPoint scanPoint : scanPoints) {
			if (isTerminated()) {
				return;
			}
			processTeamLevelDismissForScanPoint(scanPoint);
		}
	}

	private void processTeamLevelDismissForScanPoint(ScanPoint scanPoint) {
		List<RawTeamLevelDismiss> rawTeamLevelDismissList = MysqlDatabaseAdapter.getConnectedInstance()
				.loadRawTeamLevelDismiss(scanPoint.getScanPointId());
		List<TeamLevelDismiss> recordsToSave = prepareRecordsToSave(rawTeamLevelDismissList, scanPoint);
		saveTeamLevelDismissRecords(recordsToSave);
	}

	private List<TeamLevelDismiss> prepareRecordsToSave(List<RawTeamLevelDismiss> sourceList, ScanPoint scanPoint) {
		List<TeamLevelDismiss> result = new ArrayList<TeamLevelDismiss>();
		for (RawTeamLevelDismiss rawTeamLevelDismiss : sourceList) {

			//!!!!! TODO check record date time for two PK equal records

			TeamLevelDismiss teamLevelDismiss = copyTeamLevelDismiss(rawTeamLevelDismiss);
			String message = buildSuccessPrefix(scanPoint, rawTeamLevelDismiss.getTeam())
					+ teamLevelDismiss.buildSuccesMessage();
			logger.info(message);
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

	private String buildSuccessPrefix(ScanPoint scanPoint, Team team) {
		return "SUCCESS building TeamLevelDismiss " + buildPrefix(scanPoint, team) + "user dismissed ";
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
