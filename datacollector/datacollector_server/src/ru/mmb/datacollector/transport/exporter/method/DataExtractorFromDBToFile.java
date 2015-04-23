package ru.mmb.datacollector.transport.exporter.method;

import java.io.BufferedWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataExtractorFromDBToFile extends DataExtractorFromDB {
	private final BufferedWriter writer;

	public DataExtractorFromDBToFile(BufferedWriter writer) throws SQLException, Exception {
		super();
		this.writer = writer;
	}

	@Override
	protected void exportRow(ResultSet rs) throws Exception {
		String rowToExport = null;
		try {
			rowToExport = getCurrentTable().generateExportRowString(rs);
		} catch (Exception e) {
		}
		if (rowToExport != null) {
			writer.write(rowToExport);
			writer.newLine();
		}
	}
}
