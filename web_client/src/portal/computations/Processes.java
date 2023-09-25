package portal.computations;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Processes HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Processes extends PortalBase {

    /**
     * Creates a new Process
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Processes() throws IOException {
        super("/processes.jsp", "computations", "processes");
    }
}