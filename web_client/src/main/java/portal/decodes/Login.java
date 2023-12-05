package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Login HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Login extends PortalBase {

    /**
     * Creates a new Login
     * @throws IOException 
     */
    public Login() throws IOException {
        super("/login.jsp", "decodes", "login");
    }
}