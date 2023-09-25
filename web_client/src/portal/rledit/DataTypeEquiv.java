package portal.rledit;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the DataTypeEquiv HttpServlet (NOTE - This is not used in the web client, as it was decided this page is not used by any users).
 *
 * @author Will Jonassen
 *
 */
public class DataTypeEquiv extends PortalBase {

    /**
     * Creates a new DataTypeEquiv
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public DataTypeEquiv() throws IOException {
        super("/data_type_equiv.jsp", "rledit", "data_type_equiv");
    }
}