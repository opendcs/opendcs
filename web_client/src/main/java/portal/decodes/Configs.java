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
     * @throws IOException 
     */
    public Configs() throws IOException {
        super("/configs.jsp", "decodes", "configs");
    }
}