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
     * @throws IOException 
     */
    public Processes() throws IOException {
        super("/processes.jsp", "computations", "processes");
    }
}