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
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Presentation() throws IOException {
        super("/presentation.jsp", "decodes", "presentation");
    }
}