package org.opendcs.logging;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JavaUtilLoggingBridge extends ilex.util.Logger
{    
    private static final Logger logger = Logger.getLogger("org.opendcs.bridge_logger");

    public JavaUtilLoggingBridge()
    {
        super("bridge logger");
    }

    @Override
    public void close()
    {
        // nothing to do.
    }

    @Override
    public void doLog(int priority, String text)
    {
        LogRecord rec = new LogRecord(mapPriorityToLevel(priority),text);
        rec.setMillis(new Date().getTime());
        rec.setThreadID((int)Thread.currentThread().getId());
        setCaller(rec);
        logger.log(rec);
    }

    private void setCaller(LogRecord rec)
    {
        StackTraceElement stes[] = Thread.currentThread().getStackTrace();
        if (stes.length <= 3)
        {
            rec.setSourceClassName("Unknown Class.");
            rec.setSourceMethodName("Unknown Method.");
        }
        else
        {
            for(int i = 3; i < stes.length; i++)
            {
                StackTraceElement ste = stes[i];
                if(ste.getMethodName().equalsIgnoreCase("doLog")
                || ste.getMethodName().equalsIgnoreCase("log"))
                {
                    continue;
                }
                // now we've found the what actually called the logger
                rec.setLoggerName("bridge:"+ste.getClassName());
                rec.setSourceClassName(ste.getClassName());
                rec.setSourceMethodName(ste.getMethodName());
            }
            
            
        }

    }
    

    public Level mapPriorityToLevel(int priority)
    {
        switch(priority)
        {
            case ilex.util.Logger.E_FAILURE: return Level.SEVERE;
            case ilex.util.Logger.E_WARNING: return Level.WARNING;
		    case ilex.util.Logger.E_INFORMATION: return Level.INFO;
		    case ilex.util.Logger.E_DEBUG1: return Level.FINE;
            case ilex.util.Logger.E_DEBUG2: return  Level.FINER;
		    case ilex.util.Logger.E_DEBUG3: return Level.FINEST;
            default: return Level.INFO;
        }
    }
}
