package ru.mmb.datacollector.transport.exporter;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Date;

import ru.mmb.datacollector.transport.exporter.method.DataExtractorFromDB;
import ru.mmb.datacollector.transport.exporter.method.DataExtractorFromDBToFile;
import ru.mmb.datacollector.transport.exporter.method.DataExtractorFromDBToJson;
import ru.mmb.datacollector.transport.exporter.method.ExportDataMethod;
import ru.mmb.datacollector.transport.exporter.method.ExportDataMethodJson;
import ru.mmb.datacollector.transport.exporter.method.ExportDataMethodTxt;
import ru.mmb.datacollector.util.DateFormat;

public class DataExporter {
	private final Date exportDate;
	private final ExportState exportState;
	private final ExportFormat exportFormat;

	private BufferedWriter writer;

	public DataExporter(ExportState exportState, ExportFormat exportFormat) {
		this.exportDate = new Date();
		this.exportState = exportState;
		this.exportFormat = exportFormat;
	}

	public ExportResult exportData(boolean exportWithRaw) throws Exception {
		String fileName = generateFileName();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writer = new BufferedWriter(new OutputStreamWriter(baos, "UTF8"));
		try {
			createExportDataMethod().exportData(exportWithRaw);
		} finally {
			writer.close();
		}
		return new ExportResult(fileName, baos.toByteArray());
	}

	private ExportDataMethod createExportDataMethod() throws Exception {
		if (exportFormat == ExportFormat.TXT) {
			return new ExportDataMethodTxt(exportState, createDataExtractor(), writer);
		} else {
			return new ExportDataMethodJson(exportState, (DataExtractorFromDBToJson) createDataExtractor(), writer);
		}
	}

	private DataExtractorFromDB createDataExtractor() throws SQLException, Exception {
		if (exportFormat == ExportFormat.TXT) {
			return new DataExtractorFromDBToFile(writer);
		} else {
			return new DataExtractorFromDBToJson();
		}
	}

	private String generateFileName() {
		return "exp_server_FULL_" + DateFormat.format(exportDate) + "." + exportFormat.getFileExtension();
	}

	public class ExportResult {
		private final String fileName;
		private final byte[] fileBody;

		public ExportResult(String fileName, byte[] fileBody) {
			this.fileName = fileName;
			this.fileBody = fileBody;
		}

		public String getFileName() {
			return fileName;
		}

		public byte[] getFileBody() {
			return fileBody;
		}
	}
}
