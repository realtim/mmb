package ru.mmb.datacollector.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.importer.Importer;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

/**
 * Servlet implementation class LoadDataFromTabletServlet
 */
@WebServlet(name = "loadDataFromTablet", urlPatterns = { "/secure/loadDataFromTablet" })
public class LoadDataFromTabletServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(LoadDataFromTabletServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoadDataFromTabletServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		try {
			String data = request.getParameter("data");
			logger.trace("data received: " + data);
			logger.debug("data received size: " + data.length());

			String jsonPackage = decodeJsonPackage(data);
			importJsonPackage(jsonPackage);
		} catch (Exception e) {
			logger.error("data processing error: " + e.getMessage());
			logger.debug("error trace: ", e);
		}
	}

	private String decodeJsonPackage(String data) throws IOException {
		String result = null;
		byte[] decodedZip = Base64.decodeBase64(data);
		GZIPInputStream zipInput = new GZIPInputStream(new ByteInputStream(decodedZip, decodedZip.length));
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int bytesRead = zipInput.read(buffer);
			while (bytesRead != -1) {
				baos.write(buffer, 0, bytesRead);
				bytesRead = zipInput.read(buffer);
			}
			result = new String(baos.toByteArray());
			logger.trace("data unzipped: " + result);
			logger.debug("data unzipped size: " + result.length());
		} finally {
			zipInput.close();
		}
		return result;
	}

	private void importJsonPackage(String jsonPackage) throws Exception {
		new Importer(new ImportState()).importPackage(jsonPackage);
	}
}
