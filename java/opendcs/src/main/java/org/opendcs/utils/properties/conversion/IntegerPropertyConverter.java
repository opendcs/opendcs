package org.opendcs.utils.properties.conversion;

import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = PropertyConverter.class)
public final class IntegerPropertyConverter implements PropertyConverter<Integer>
{

    @Override
    public Class<Integer> getType()
    {
        return Integer.class;
    }

    @Override
    public Integer fromString(String value)
    {
        return Integer.parseInt(value, 10);
    }

    @Override
    public String toString(Integer value)
    {
        return value.toString();
    }
    
}
