package portal.computations;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Computation HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Computations extends PortalBase {

    /**
     * Creates a new Computation
     * @throws IOException 
     */
    public Computations() throws IOException {
        super("/computations.jsp", "computations", "computations");
    }
}