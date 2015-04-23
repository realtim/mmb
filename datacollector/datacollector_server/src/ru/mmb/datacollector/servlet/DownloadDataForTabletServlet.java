package ru.mmb.datacollector.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.transport.exporter.DataExporter;
import ru.mmb.datacollector.transport.exporter.DataExporter.ExportResult;
import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportState;

/**
 * Servlet implementation class DownloadDataForTabletServlet
 */
@WebServlet("/secure/downloadDataForTablet")
public class DownloadDataForTabletServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(DownloadDataForTabletServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DownloadDataForTabletServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		byte[] bytesToSend = null;
		try {
			ExportResult exportResult = new DataExporter(new ExportState(), ExportFormat.JSON).exportData();
			bytesToSend = encodeFileBody(exportResult.getFileBody());
		} catch (Exception e) {
			logger.error("data export error: " + e.getMessage());
		}
		if (bytesToSend == null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/plain; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("error");
			out.close();
			return;
		}

		response.setContentType("text/plain");
		ServletOutputStream out = response.getOutputStream();
		out.write(bytesToSend);
		out.flush();
		out.close();
	}

	private byte[] encodeFileBody(byte[] fileBody) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(baos);
		gzip.write(fileBody, 0, fileBody.length);
		gzip.finish();
		gzip.close();
		return Base64.encodeBase64String(baos.toByteArray()).getBytes();
	}
}
