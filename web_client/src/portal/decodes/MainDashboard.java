package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the MainDashboard HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class MainDashboard extends PortalBase {

    /**
     * Creates a new MainDashboard
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public MainDashboard() throws IOException {
        super("/main_dashboard.jsp", "decodes", "main_dashboard");
    }
}