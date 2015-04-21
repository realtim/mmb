package ru.mmb.datacollector.converter.engine;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.converter.DataConverterThread;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;

public class AbstractConvertationEngine {
	private static final Logger logger = LogManager.getLogger(AbstractConvertationEngine.class);

	private final DataConverterThread owner;

	protected Connection conn = null;
	protected Statement batchStatement = null;

	public AbstractConvertationEngine(DataConverterThread owner) {
		this.owner = owner;
	}

	public boolean isTerminated() {
		return owner.isTerminated();
	}

	protected String buildPrefix(ScanPoint scanPoint, Team team) {
		return "scanPoint \"" + scanPoint.getScanPointName() + "\" and team " + team.getTeamNum() + " \""
				+ team.getTeamName() + "\" ";
	}

	protected void commitBatch() throws SQLException {
		try {
			batchStatement.executeBatch();
			conn.commit();
			logger.debug("batch committed");
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		}
	}
}
