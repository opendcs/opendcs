package org.opendcs.logging.jul;

import java.util.ArrayList;
import java.util.logging.Filter;
import java.util.logging.LogRecord;

import decodes.cwms.CwmsConnectionPool;

public class OpenDcsFilter implements Filter
{
    
    private static final String HIDE_BY_DEFAULT[] = new String[] {"java.awt","sun.awt", "javax","sun","usace", "cwmsdb", "rma", "hec", "wcds", "com.rma"};
	private static final String HIDE_IF_CONN_TRACE_NOT_SET[] = new String[] {"org.jooq","oracle", "usace.cwms"};
    private boolean trace = Boolean.parseBoolean(System.getProperty(CwmsConnectionPool.POOL_TRACE_PROPERTY,"false"));

    private ArrayList<String> filterOut = new ArrayList<>();


    public OpenDcsFilter()
    {
        for (String path: HIDE_BY_DEFAULT)
        {
            filterOut.add(path);
        }

        if (!trace)
        {
            for (String path: HIDE_IF_CONN_TRACE_NOT_SET)
            {
                filterOut.add(path);
            }
        }
    }

    @Override
    public boolean isLoggable(LogRecord record)
    {
        for (String path: filterOut)
        {
            if (record.getLoggerName().startsWith(path))
            {
                return false;
            }
        }
        return true;
    }
    
}
