package ru.mmb.datacollector.transport.exporter.method;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.db.ConnectionPool;
import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.transport.exporter.ExportState;

public abstract class DataExtractorFromDB {
	private static final Logger logger = LogManager.getLogger(DataExtractorFromDB.class);

	private MetaTable currentTable = null;

	private Connection conn = null;

	protected abstract void exportRow(ResultSet rs) throws Exception;

	public DataExtractorFromDB() throws SQLException, Exception {
		conn = ConnectionPool.getInstance().getConnection();
	}

	public void setCurrentTable(MetaTable metaTable) {
		currentTable = metaTable;
	}

	public MetaTable getCurrentTable() {
		return currentTable;
	}

	public boolean hasRecordsToExport() throws SQLException {
		if (currentTable == null)
			return false;

		String sql = currentTable.generateCheckNewRecordsSQL();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				int recordCount = rs.getInt(1);
				return recordCount > 0;
			} else {
				return false;
			}
		} finally {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				logger.trace("resources release failed", e);
			}
		}
	}

	public void exportNewRecords(ExportState exportState) throws Exception {
		String selectSql = currentTable.generateSelectNewRecordsSQL();

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(selectSql);
			while (rs.next() && !exportState.isTerminated()) {
				exportRow(rs);
			}
		} finally {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				logger.trace("resources release failed", e);
			}
		}
	}

	public void releaseResources() {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (SQLException e) {
			logger.trace("resources release failed", e);
		}
	}
}
