package org.sakaiproject.login.tool;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.SessionManager;

/**
 * This servlet is useful when you want to have another HTTP request made for logout as your container based
 * authentication needs to do some cleanup and you can't do that on a normal logout. We redirect to this servlet
 * so that we can be sure that we get the additional HTTP request on a known URL.
 */
public class ContainerLogout extends HttpServlet {

	/** Our log (commons). */
	private static final Logger M_log = LoggerFactory.getLogger(ContainerLogin.class);

	private ServerConfigurationService serverConfigurationService;
	private UsageSessionService usageSessionService;
	private SessionManager sessionManager;

	/**
	 * Access the Servlet's information display.
	 * 
	 * @return servlet information.
	 */
	public String getServletInfo()
	{
		return "Sakai Container Logout";
	}

	/**
	 * Initialize the servlet.
	 * 
	 * @param config
	 *        The servlet config.
	 * @throws ServletException
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		M_log.debug("init()");
		serverConfigurationService = (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
		usageSessionService = (UsageSessionService) ComponentManager.get(UsageSessionService.class);
		sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
	}

	/**
	 * Shutdown the servlet.
	 */
	public void destroy()
	{
		M_log.debug("destroy()");

		super.destroy();
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		// get the session
		Session session = sessionManager.getCurrentSession();
		String returnUrl = serverConfigurationService.getString("login.container.logout.url", null);
		
		// if we end up with nowhere to go, go to the portal
		if (returnUrl == null)
		{
			M_log.warn("login.container.logout.url isn't set, to use container logout it should be.");
			returnUrl = (String)session.getAttribute(Tool.HELPER_DONE_URL);
			if (returnUrl == null || "".equals(returnUrl))
			{
				M_log.debug("complete: nowhere set to go, going to portal");
				returnUrl = serverConfigurationService.getPortalUrl();
			}
		}

		usageSessionService.logout();

		// redirect to the done URL
		res.sendRedirect(res.encodeRedirectURL(returnUrl));

	}
}
