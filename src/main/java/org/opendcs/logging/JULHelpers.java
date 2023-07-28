package org.opendcs.logging;

import java.util.logging.Level;

public class JULHelpers
{
    /**
     * Convert between logging system level values
     * @param ilexLevel desired ilex logging level
     * @return equivalent JUL Level
     */
    public static Level ilexLevelToJulLevel(int ilexLevel)
    {
        switch(ilexLevel)
        {
            case ilex.util.Logger.E_WARNING: return Level.WARNING;
            case ilex.util.Logger.E_DEBUG1: return Level.FINE;
            case ilex.util.Logger.E_DEBUG2: return Level.FINER;
            case ilex.util.Logger.E_DEBUG3: return Level.FINEST;
            case ilex.util.Logger.E_FAILURE: // intentional pass through
            case ilex.util.Logger.E_FATAL: return Level.SEVERE;
            case ilex.util.Logger.E_INFORMATION: ;// intentional pass through (includes DEFAULT MIN)
            default: return Level.INFO;
        }
    }
    
}
