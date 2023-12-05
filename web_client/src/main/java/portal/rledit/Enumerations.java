package portal.rledit;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Enumerations HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Enumerations extends PortalBase {
    /**
     * Creates a new Enumeration
     * @throws IOException 
     */
    public Enumerations() throws IOException {
        super("/enumerations.jsp", "rledit", "enumerations");
    }
}