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
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public EuConversions() throws IOException {
        super("/eu_conversions.jsp", "rledit", "eu_conversions");
    }
}