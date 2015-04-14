package ru.mmb.datacollector.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

/**
 * Servlet implementation class CheckLoggedInServlet
 */
@WebServlet(name = "checkLoggedIn", urlPatterns = { "/unchecked/checkLoggedIn" })
public class CheckLoggedInServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(CheckLoggedInServlet.class);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CheckLoggedInServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean userLoggedIn = request.getUserPrincipal() != null;
		String userName = "";
		if (userLoggedIn) {
			userName = request.getUserPrincipal().getName();
			logger.debug("user logged in: " + userName);
		} else {
			logger.debug("user NOT logged in");
		}
		CheckLoggedInResult result = new CheckLoggedInResult(userLoggedIn, userName);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(new Gson().toJson(result));
	}

	private class CheckLoggedInResult {
		@SuppressWarnings("unused")
		private final boolean userLoggedIn;
		@SuppressWarnings("unused")
		private final String userName;

		public CheckLoggedInResult(boolean userLoggedIn, String userName) {
			this.userLoggedIn = userLoggedIn;
			this.userName = userName;
		}
	}
}
