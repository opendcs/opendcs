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
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Seasons() throws IOException {
        super("/seasons.jsp", "rledit", "seasons");
    }
}