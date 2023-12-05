package portal.rledit;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the EuConversions HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class EuConversions extends PortalBase {

    /**
     * Creates a new EuConversion
     * @throws IOException 
     */
    public EuConversions() throws IOException {
        super("/eu_conversions.jsp", "rledit", "eu_conversions");
    }
}