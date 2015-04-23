package ru.mmb.datacollector.converter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.converter.DataConverter.ConvertRequest;
import ru.mmb.datacollector.converter.engine.TeamLevelDismissEngine;
import ru.mmb.datacollector.converter.engine.TeamLevelPointsEngine;
import ru.mmb.datacollector.db.ConnectionPool;

public class DataConverterThread extends Thread {
	private static final Logger logger = LogManager.getLogger(DataConverterThread.class);

	private final BlockingQueue<ConvertRequest> converterQueue;
	private volatile boolean terminated = false;

	public DataConverterThread(BlockingQueue<ConvertRequest> converterQueue) {
		super("data_converter_thread");
		this.converterQueue = converterQueue;
	}

	public void terminate() {
		terminated = true;
		interrupt();
		// wait some time
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}

	public boolean isTerminated() {
		return terminated;
	}

	@Override
	public void run() {
		// when application destroy listener fires, logger system is already shut down
		logger.info("data converter thread started");
		while (!isTerminated()) {
			try {
				converterQueue.take();
				performConvertation();
			} catch (InterruptedException e) {
				System.out.println("data converter thread interrupted");
			}
		}
		System.out.println("data converter thread stopped");
	}

	private void performConvertation() {
		if (isTerminated()) {
			return;
		}
		logger.info("data convertation started");
		deleteRecordsFromTargetTables();
		logger.info("tables cleared");
		new TeamLevelPointsEngine(this).convertTeamLevelPoints();
		new TeamLevelDismissEngine(this).convertTeamLevelDismiss();
		logger.info("data convertation finished");
	}

	private void deleteRecordsFromTargetTables() {
		deleteRecordsFromTable("TeamLevelPoints");
		deleteRecordsFromTable("TeamLevelDismiss");
	}

	private void deleteRecordsFromTable(String tableName) {
		try {
			Connection conn = ConnectionPool.getInstance().getConnection();
			Statement stmt = null;
			try {
				String sql = "delete from `" + tableName + "`";
				stmt = conn.createStatement();
				stmt.executeUpdate(sql);
				conn.commit();
			} catch (SQLException e1) {
				conn.rollback();
			} finally {
				try {
					if (stmt != null) {
						stmt.close();
						stmt = null;
					}
					if (conn != null) {
						conn.close();
						conn = null;
					}
				} catch (SQLException e2) {
					// resource release failed
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.debug("error trace", e);
		}
	}
}
