package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Routing HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Routing extends PortalBase {

    /**
     * Creates a new Routing
     * @throws IOException 
     */
    public Routing() throws IOException {
        super("/routing.jsp", "decodes", "routing");
    }
}