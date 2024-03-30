package org.opendcs.utils.properties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.opendcs.spi.properties.PropertyValueProvider;

/**
 * Handle baseline variables required by opendcs for various operations.
 * These variables exist in the System properties and are set at program start.
 * They could also be called using ${java.&lt;property name&gt;} but providing them
 * in this way saves the developers and the users a lot of change.
 */
public class DcsVariablesProvider implements PropertyValueProvider
{
    private static String DCS_VARS[] = new String[] {"DCSTOOL_HOME",
                                                     "DCSTOOL_USERDIR",
                                                     "DCSTOOL_INSTALL_DIR",
                                                     "DECODES_INSTALL_DIR"
                                                    };

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
