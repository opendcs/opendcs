package org.opendcs.utils.logging;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

public class SLF4JLogger extends AbstractLogger
{
    private String name;

    public SLF4JLogger(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return ilex.util.Logger.instance().getMinLogPriority() < ilex.util.Logger.E_INFORMATION;
    }

    @Override
    public boolean isDebugEnabled(Marker marker)
    {
        return isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return ilex.util.Logger.instance().getMinLogPriority() < ilex.util.Logger.E_FAILURE;
    }

    @Override
    public boolean isErrorEnabled(Marker arg0)
    {
        return isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return ilex.util.Logger.instance().getMinLogPriority() < ilex.util.Logger.E_WARNING;
    }

    @Override
    public boolean isInfoEnabled(Marker arg0)
    {
        return isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled()
    {
        return ilex.util.Logger.instance().getMinLogPriority() <= ilex.util.Logger.E_DEBUG3;
    }

    @Override
    public boolean isTraceEnabled(Marker arg0)
    {
        return isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return ilex.util.Logger.instance().getMinLogPriority() <= ilex.util.Logger.E_WARNING;
    }

    @Override
    public boolean isWarnEnabled(Marker arg0)
    {
        return isWarnEnabled();
    }

    @Override
    protected String getFullyQualifiedCallerName()
    {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] args, Throwable ex)
    {
        int ilexLevel = slf4jToIlexLevel(level);
        ilex.util.Logger.instance().log(ilexLevel,MessageFormatter.arrayFormat(msg, args).getMessage());
        PrintStream ps = null;
        OutputStream out = null;
        if (ilex.util.Logger.instance().getMinLogPriority() <= ilexLevel
            && ex != null)
        {
            out = new ByteArrayOutputStream(ex.getStackTrace().length*50); // assume about 50 characters per line
            ps = new PrintStream(out);
            ex.printStackTrace(ps);
            ilex.util.Logger.instance().log(ilexLevel, out.toString());
        }
    }

    private int slf4jToIlexLevel(Level level)
    {
        {
            switch(level)
            {
                case WARN: return ilex.util.Logger.E_WARNING;
                case DEBUG: return ilex.util.Logger.E_DEBUG1;
                case TRACE: return ilex.util.Logger.E_DEBUG3;
                case ERROR: return ilex.util.Logger.E_FAILURE;
                case INFO: ;// intentional pass through
                default: return ilex.util.Logger.E_INFORMATION;
            }
        }
    }
}
