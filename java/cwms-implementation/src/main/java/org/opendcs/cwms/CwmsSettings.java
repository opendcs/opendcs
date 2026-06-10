package org.opendcs.cwms;

import java.util.Properties;

import org.opendcs.settings.api.OpenDcsSettings;

import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
 * Hold any CWMS Specific operational settings.
 * 
 * NOTE: Intentionally empty. Placeholder for future work.
 */
public final class CwmsSettings implements PropertiesOwner, OpenDcsSettings
{

    @SuppressWarnings("java:S1172") // future work
    public CwmsSettings(Properties props)
    {
        // Future work.
    }

    public CwmsSettings()
    {
    }

    @Override
    public PropertySpec[] getSupportedProps()
    {
        return new PropertySpec[0];
    }

    @Override
    public boolean additionalPropsAllowed()
    {
        return false;
    }
    
}
