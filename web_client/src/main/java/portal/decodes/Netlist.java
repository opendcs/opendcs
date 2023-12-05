package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Netlist HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Netlist extends PortalBase {

    /**
     * Creates a new Netlist
     * @throws IOException 
     */
    public Netlist() throws IOException {
        super("/netlist.jsp", "decodes", "netlist");
    }
}