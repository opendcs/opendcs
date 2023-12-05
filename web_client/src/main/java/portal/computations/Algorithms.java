package portal.computations;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Algorithm HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Algorithms extends PortalBase {

    /**
     * Creates a new Algorithm
     * @throws IOException 
     */
    public Algorithms() throws IOException {
        super("/algorithms.jsp", "computations", "algorithms");
    }
}