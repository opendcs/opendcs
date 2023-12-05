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
     * @throws IOException 
     */
    public EngineeringUnits() throws IOException {
        super("/engineering_units.jsp", "rledit", "engineering_units");
    }
}