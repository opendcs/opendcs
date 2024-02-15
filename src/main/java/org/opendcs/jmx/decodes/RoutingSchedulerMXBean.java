package org.opendcs.jmx.decodes;

import java.util.List;

public interface RoutingSchedulerMXBean
{
    public int getNumberActiveRoutingSpecs();
    public List<String> getScheduledExecutives();
}
