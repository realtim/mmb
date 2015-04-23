package ru.mmb.datacollector.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.conf.Settings;
import ru.mmb.datacollector.converter.DataConverter;
import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.importer.Importer;

/**
 * Servlet implementation class LoadDataFromFileServlet
 */
@WebServlet("/secure/loadDataFromFile")
public class LoadDataFromFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(LoadDataFromFileServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoadDataFromFileServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		logger.debug("starting file upload");
		if (!ServletFileUpload.isMultipartContent(request)) {
			writeResponse(response, "ОШИБКА при загрузке файла");
			return;
		}
		try {
			// Create a factory for disk-based file items
			DiskFileItemFactory factory = new DiskFileItemFactory();
			// Configure a repository (to ensure a secure temp location is used)
			File repository = new File(Settings.getInstance().getFileUploadTempDir());
			factory.setRepository(repository);
			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
			// Parse the request
			List<FileItem> items = upload.parseRequest(request);
			for (FileItem item : items) {
				if (!item.isFormField()) {
					processUploadingFile(item);
				}
			}
			writeResponse(response, "УСПЕХ файл загружен");
		} catch (Exception e) {
			logger.debug("ERROR uploading file", e);
			writeResponse(response, "ОШИБКА при загрузке файла");
		}
	}

	private void writeResponse(HttpServletResponse response, String message) throws IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(message);
	}

	private void processUploadingFile(FileItem item) throws Exception {
		InputStream fileContent = item.getInputStream();
		logger.debug("stream size: " + item.getSize());
		byte[] inputBytes = new byte[(int) item.getSize()];
		ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes);
		try {
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = fileContent.read(bytes)) != -1) {
				inputBuffer.put(bytes, 0, read);
			}
			logger.debug("file loaded to byte buffer");
		} finally {
			fileContent.close();
		}
		String jsonString = new String(inputBytes);
		logger.trace("received string:\n" + jsonString);
		importJsonPackage(jsonString);
		logger.debug("import finished");
		// send request to recalculate RAW data
		DataConverter.offerRequest();
	}

	private void importJsonPackage(String jsonPackage) throws Exception {
		new Importer(new ImportState()).importPackage(jsonPackage);
	}
}
