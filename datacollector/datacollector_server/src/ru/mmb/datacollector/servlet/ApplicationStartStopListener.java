package ru.mmb.datacollector.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.mmb.datacollector.conf.ServletConfigurationAdapter;
import ru.mmb.datacollector.conf.Settings;
import ru.mmb.datacollector.converter.DataConverter;
import ru.mmb.datacollector.db.MysqlDatabaseAdapter;

/**
 * Application Lifecycle Listener implementation class
 * ApplicationStartStopListener
 *
 */
@WebListener
public class ApplicationStartStopListener implements ServletContextListener {
	private static final Logger logger = LogManager.getLogger(ApplicationStartStopListener.class);

	/**
	 * Default constructor.
	 */
	public ApplicationStartStopListener() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		ServletContext context = servletContextEvent.getServletContext();
		logger.info("web app initializing");
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
		Settings.getInstance().setFileUploadTempDir(
				getContextParameter(context, Settings.FILE_UPLOAD_TEMP_DIR, "c:\\tmp"));
		Settings.getInstance().setTransportUserId(getContextParameter(context, Settings.TRANSPORT_USER_ID, ""));
		Settings.getInstance().setTransportUserPassword(
				getContextParameter(context, Settings.TRANSPORT_USER_PASSWORD, ""));
		logger.info("settings loaded" + Settings.getInstance().toString());
		ServletConfigurationAdapter.init();
		MysqlDatabaseAdapter.init();
		DataConverter.init();
		logger.info("web app initialized");
	}

	private String getContextParameter(ServletContext context, String parameterName, String defaultValue) {
		if (context.getInitParameter(parameterName) != null) {
			return context.getInitParameter(parameterName);
		} else {
			return defaultValue;
		}
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		System.out.println("web app stopping");
		DataConverter.stop();
		System.out.println("web app stopped");
	}
}
