package portal.rledit;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Seasons HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Seasons extends PortalBase {

    /**
     * Creates a new Season
     * @throws IOException 
     */
    public Seasons() throws IOException {
        super("/seasons.jsp", "rledit", "seasons");
    }
}