package org.opendcs.utils.logging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class LogFilter {
    /** NOTE: this class is used in the logger creation path. Do no logging. */
    Set<String> logPathsToFilter = new HashSet<>();

    public LogFilter(String filterFile)
    {

        try 
        {
            File f = new File(filterFile);
            try (BufferedReader br = new BufferedReader(new FileReader(f));)
            {
                String line = null;
                while((line = br.readLine()) != null)
                {
                    String tmp = line.trim();
                    if (!tmp.startsWith("#"))
                    logPathsToFilter.add(tmp);
                }
            }
        }
        catch (Exception ex)
        {
            /* do nothing, most likely file doesn't exist */
        }
    }

    boolean canLog(String loggerName)
    {
        boolean retVal = true;
        if(!logPathsToFilter.isEmpty())
        {
            retVal = false;
        }
        else
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
