package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Sources HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Sources extends PortalBase {

    /**
     * Creates a new Source
     * @throws IOException 
     */
    public Sources() throws IOException {
        super("/sources.jsp", "decodes", "sources");
    }
}