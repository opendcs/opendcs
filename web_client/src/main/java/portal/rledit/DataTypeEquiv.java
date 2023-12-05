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
     * @throws IOException 
     */
    public DataTypeEquiv() throws IOException {
        super("/data_type_equiv.jsp", "rledit", "data_type_equiv");
    }
}