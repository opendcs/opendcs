package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Sites HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Sites extends PortalBase {

    /**
     * Creates a new Site
     * @throws IOException 
     */
    public Sites() throws IOException {
        super("/sites.jsp", "decodes", "sites");
    }
}