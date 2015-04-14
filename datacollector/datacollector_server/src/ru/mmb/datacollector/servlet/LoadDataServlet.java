package ru.mmb.datacollector.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.conf.ServletConfigurationAdapter;
import ru.mmb.datacollector.conf.Settings;

/**
 * Servlet implementation class LoadDataServlet
 */
@WebServlet(name = "loadData", urlPatterns = { "/secure/loadData" }, loadOnStartup = 1)
public class LoadDataServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(LoadDataServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoadDataServlet() {
		super();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		ServletContext context = config.getServletContext();
		logger.info("servlet initializing");
		Settings.getInstance().setCurrentRaidId(
				Integer.parseInt(getContextParameter(context, Settings.CURRENT_RAID_ID, "-1")));
		Settings.getInstance().setDbPoolSize(
				Integer.parseInt(getContextParameter(context, Settings.MYSQL_CONNECTION_POOL_SIZE, "10")));
		Settings.getInstance().setDbConnectionString(
				getContextParameter(context, Settings.MYSQL_CONNECTION_STRING,
						"jdbc:mysql://localhost:3306/datacollector"));
		Settings.getInstance().setDbUserName(
				getContextParameter(context, Settings.MYSQL_CONNECTION_USERNAME, "datacollector"));
		Settings.getInstance().setDbPassword(
				getContextParameter(context, Settings.MYSQL_CONNECTION_PASSWORD, "datacollector"));
		logger.info("settings loaded" + Settings.getInstance().toString());
		ServletConfigurationAdapter.init();
		logger.info("configuration adapter initialized");
		logger.info("servlet initialized");
	}

	private String getContextParameter(ServletContext context, String parameterName, String defaultValue) {
		if (context.getInitParameter(parameterName) != null) {
			return context.getInitParameter(parameterName);
		} else {
			return defaultValue;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		// TODO Auto-generated method stub
	}
}
