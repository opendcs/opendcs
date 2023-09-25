package portal.rledit;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the EngineeringUnits HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class EngineeringUnits extends PortalBase {

    /**
     * Creates a new EngineeringUnit
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public EngineeringUnits() throws IOException {
        super("/engineering_units.jsp", "rledit", "engineering_units");
    }
}