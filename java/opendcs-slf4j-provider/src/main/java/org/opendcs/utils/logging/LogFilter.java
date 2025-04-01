package org.opendcs.utils.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class LogFilter
{
    /** NOTE: this class is used in the logger creation path. Do no logging. */
    private final Set<String> logPathsToFilter;

    LogFilter(String filterFile)
    {
        Set<String> filters = new HashSet<>();
        try 
        {
            filters = Files.readAllLines(Paths.get(filterFile)).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("#"))
                    .collect(Collectors.toSet());
        }
        catch (IOException | RuntimeException ex) //NOSONAR
        {
            /* do nothing, most likely file doesn't exist */
        }
        logPathsToFilter = filters;
    }

    /**
     * Search to list excluded paths to see if the provided log name 
     * matches against a simple String::startsWith .
     * <p>
     * If a log names starts with one of the exclude string, we don't log 
     * its messages.
     */
    boolean canLog(String loggerName)
    {
        boolean retVal = true;
        if(!logPathsToFilter.isEmpty())
        {
            for (String filterPath: logPathsToFilter)
            {
                if (loggerName.startsWith(filterPath))
                {
                    retVal = false;
                    break;
                }
            }
        }
        return retVal;
    }

}
