package ru.mmb.datacollector.transport.importer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.db.ConnectionPool;
import ru.mmb.datacollector.model.meta.MetaTable;

/**
 * Data synchronization disabled.<br>
 * All records MUST be removed from table before import.<br>
 * All rows from import package will be imported without any checks.<br>
 * 
 * @author yweiss
 */
public class DataSaver {
	private static final Logger logger = LogManager.getLogger(DataSaver.class);

	private MetaTable currentTable = null;

	private Connection conn = null;
	private Statement batchStatement = null;

	public DataSaver() throws SQLException, Exception {
		conn = ConnectionPool.getInstance().getConnection();
		batchStatement = conn.createStatement();
	}

	public void setCurrentTable(MetaTable metaTable) {
		currentTable = metaTable;
	}

	public void saveRecordToDB(JSONObject tableRow) throws JSONException, SQLException {
		if (currentTable == null)
			return;
		if (tableRow == null)
			return;

		// If table is cleared before import, then no PK violation possible.
		if (currentTable.needClearBeforeImport()) {
			insertRecord(tableRow);
			return;
		}

		ImportToDBAction action = getImportToDBAction(tableRow);
		if (action == ImportToDBAction.UPDATE) {
			updateRecord(tableRow);
		} else if (action == ImportToDBAction.INSERT) {
			insertRecord(tableRow);
		}
	}

	private ImportToDBAction getImportToDBAction(JSONObject tableRow) throws JSONException, SQLException {
		if (currentTable.getUpdateDateColumnName() == null) {
			if (isRecordExists(tableRow)) {
				return ImportToDBAction.UPDATE;
			} else {
				return ImportToDBAction.INSERT;
			}
		} else {
			Date recordUpdateDate = getRecordUpdateDate(tableRow);
			if (recordUpdateDate == null) {
				// record doesn't exist, insert needed
				return ImportToDBAction.INSERT;
			}
			Date bufferUpdateDate = currentTable.getUpdateDate(tableRow);
			if (recordUpdateDate.before(bufferUpdateDate)) {
				return ImportToDBAction.UPDATE;
			} else {
				return ImportToDBAction.IGNORE;
			}
		}
	}

	private boolean isRecordExists(JSONObject tableRow) throws JSONException, SQLException {
		String sql = currentTable.generateCheckExistsSQL(tableRow);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				int recordCount = rs.getInt(1);
				return recordCount == 1;
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

	private Date getRecordUpdateDate(JSONObject tableRow) throws JSONException, SQLException {
		String sql = currentTable.generateUpdateDateSelectSQL(tableRow);
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			if (rs.next()) {
				return (Date) currentTable.getUpdateDateColumn().getValue(rs, 1);
			} else {
				return null;
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

	private void updateRecord(JSONObject tableRow) throws JSONException, SQLException {
		String sql = currentTable.generateUpdateSQL(tableRow);
		logger.trace("update SQL " + sql);
		batchStatement.addBatch(sql);
	}

	private void insertRecord(JSONObject tableRow) throws JSONException, SQLException {
		String sql = currentTable.generateInsertSQL(tableRow);
		logger.trace("insert SQL " + sql);
		batchStatement.addBatch(sql);
	}

	public void clearCurrentTable() throws SQLException {
		String sql = currentTable.generateDeleteAllRowsSQL();
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			conn.commit();
		} catch (SQLException e) {
			conn.rollback();
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
					stmt = null;
				}
			} catch (SQLException e) {
				logger.trace("resources release failed", e);
			}
		}
	}

	public void commitBatch() throws SQLException {
		try {
			batchStatement.executeBatch();
			conn.commit();
			logger.debug("batch committed");
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		}
	}

	public void releaseResources() {
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
