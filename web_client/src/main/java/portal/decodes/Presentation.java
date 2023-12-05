package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Presentation HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Presentation extends PortalBase {

    /**
     * Creates a new Presentation
     * @throws IOException 
     */
    public Presentation() throws IOException {
        super("/presentation.jsp", "decodes", "presentation");
    }
}