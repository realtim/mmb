package ru.mmb.datacollector.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.transport.exporter.DataExporter;
import ru.mmb.datacollector.transport.exporter.DataExporter.ExportResult;
import ru.mmb.datacollector.transport.exporter.ExportFormat;
import ru.mmb.datacollector.transport.exporter.ExportState;

/**
 * Servlet implementation class DownloadDataForSiteServlet
 */
@WebServlet("/secure/downloadDataForSite")
public class DownloadDataForSiteServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(DownloadDataForSiteServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DownloadDataForSiteServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ExportResult exportResult = null;
		try {
			exportResult = new DataExporter(new ExportState(), ExportFormat.TXT).exportData();
		} catch (Exception e) {
			logger.error("data export error: " + e.getMessage());
		}
		if (exportResult == null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType("text/html; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println("Ошибка экспорта данных в TXT.<br>");
			out.println("Подробности можно посмотреть в log-файлах на сервере.");
			out.close();
			return;
		}

		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment;filename=" + exportResult.getFileName());
		ServletOutputStream out = response.getOutputStream();
		out.write(exportResult.getFileBody());
		out.flush();
		out.close();
	}
}
