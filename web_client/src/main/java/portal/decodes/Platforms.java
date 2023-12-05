package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Platform HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Platforms extends PortalBase {

    /**
     * Creates a new Platform
     * @throws IOException 
     */
    public Platforms() throws IOException {
        super("/platforms.jsp", "decodes", "platforms");
    }
}