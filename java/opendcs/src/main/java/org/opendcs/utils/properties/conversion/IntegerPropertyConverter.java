package org.opendcs.utils.properties.conversion;

public final class IntegerPropertyConverter implements PropertyConverter<Integer>
{

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
