package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Config HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Configs extends PortalBase {

    /**
     * Creates a new Config
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Configs() throws IOException {
        super("/configs.jsp", "decodes", "configs");
    }
}