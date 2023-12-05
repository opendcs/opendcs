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
     * @throws IOException 
     */
    public Schedule() throws IOException {
        super("/schedule.jsp", "decodes", "schedule");
    }
}