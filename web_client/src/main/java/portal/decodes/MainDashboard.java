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
     * @throws IOException 
     */
    public MainDashboard() throws IOException {
        super("/main_dashboard.jsp", "decodes", "main_dashboard");
    }
}