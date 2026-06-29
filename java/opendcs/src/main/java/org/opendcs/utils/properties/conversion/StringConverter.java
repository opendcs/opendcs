package org.opendcs.utils.properties.conversion;

import org.openide.util.lookup.ServiceProvider;

/**
 * Could also be called "NoOp" converter. Exists to avoid edge cases in code
 */
@ServiceProvider(service = PropertyConverter.class)
public class StringConverter implements PropertyConverter<String>
{

    @Override
    public Class<String> getType()
    {
        return String.class;
    }

    @Override
    public String fromString(String value)
    {
        return value;
    }

    @Override
    public String toString(String value)
    {
        return value;
    }
}
