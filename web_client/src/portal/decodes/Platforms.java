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
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Platforms() throws IOException {
        super("/platforms.jsp", "decodes", "platforms");
    }
}