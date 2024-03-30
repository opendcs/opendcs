package org.opendcs.utils.properties;



import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.opendcs.spi.properties.PropertyValueProvider;

/**
 * Handle variables required by Current LRGS reports.
 */
public class LrgsVariablesProvider implements PropertyValueProvider
{
    private static String DCS_VARS[] = new String[] {"LRGSHOME",
                                                     "LRGSVERSION",
                                                     "SYSTEMSTAT",
                                                     "STATSTYLE",
                                                     "TZ",
                                                     "IMAGEFILE",
                                                     "HOSTNAME"};

    @Override
    public boolean canProcess(String value)
    {
        for (String v: DCS_VARS) {
            if (v.equals(value))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String processValue(String varName, Properties properties, Map<String, String> env) throws IOException
    {
        return properties.getProperty(varName, env.get(varName));
    }
    
}
