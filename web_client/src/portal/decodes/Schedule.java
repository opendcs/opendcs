package portal.decodes;

import java.io.IOException;

import portal.PortalBase;

/**
 * Represents the Schedule HttpServlet.
 *
 * @author Will Jonassen
 *
 */
public class Schedule extends PortalBase {

    /**
     * Creates a new Schedule
     *
     * @param destination    Relative path for the web page.
     * @param tool           The tool that this page falls under (DECODES, Computations, Processes), 
     *                       which will be used to highlight the corresponding sidemenu toggle..
     * @param page           The page, which the jsp will use to highlight the corresponding sidemenu item.
     * @throws IOException 
     */
    public Schedule() throws IOException {
        super("/schedule.jsp", "decodes", "schedule");
    }
}