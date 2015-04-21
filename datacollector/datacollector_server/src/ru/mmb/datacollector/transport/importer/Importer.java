package ru.mmb.datacollector.transport.importer;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;

public class Importer {
	private static final Logger logger = LogManager.getLogger(Importer.class);

	public static final int ROWS_IN_BATCH = 200;

	private final ImportState importState;

	public Importer(ImportState importState) {
		this.importState = importState;
	}

	public void importPackage(String packageJSONString) throws Exception {
		logger.debug("Import started.");

		JSONObject tables = new JSONObject(packageJSONString);

		logger.debug("Package loaded to JSON object.");

		DataSaver dataSaver = new DataSaver();
		try {
			JSONArray names = tables.names();
			for (int i = 0; i < names.length(); i++) {
				String tableName = names.getString(i);
				logger.debug("importing table: " + tableName);
				MetaTable metaTable = getMetaTable(tableName);
				if (metaTable == null) {
					logger.debug("Meta table not found.");
					continue;
				}
				dataSaver.setCurrentTable(metaTable);
				if (metaTable.needClearBeforeImport()) {
					dataSaver.clearCurrentTable();
					logger.debug("table cleared: " + tableName);
				}
				JSONArray tableRows = tables.getJSONArray(tableName);
				resetImportState(metaTable, tableRows.length());
				importTableRows(dataSaver, tableRows);
			}
		} finally {
			dataSaver.releaseResources();
		}
	}

	private MetaTable getMetaTable(String tableName) {
		return MetaTablesRegistry.getInstance().getTableByName(tableName);
	}

	private void resetImportState(MetaTable metaTable, int totalRows) {
		importState.setCurrentTable(metaTable.getTableName());
		importState.setTotalRows(totalRows);
		importState.setRowsProcessed(0);
	}

	private void importTableRows(DataSaver dataSaver, JSONArray tableRows) throws JSONException, SQLException {
		for (int j = 0; j < tableRows.length(); j++) {
			if (needSaveBatch(j)) {
				dataSaver.commitBatch();
			}
			try {
				dataSaver.saveRecordToDB(tableRows.getJSONObject(j));
			} catch (Exception e) {
				logger.error("row not imported: " + tableRows.getJSONObject(j));
				logger.debug("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
			}
			importState.incRowsProcessed();
		}
		dataSaver.commitBatch();
	}

	private boolean needSaveBatch(int j) {
		return ((j + 1) % ROWS_IN_BATCH) == 0;
	}
}
