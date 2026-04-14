package org.opendcs.utils.properties.conversion;

import org.openide.util.lookup.ServiceProvider;

import ilex.util.TextUtil;


/**
 * Uses TextUtil to allow multiple truthy values {@see TextUtil.str2boolean}
 * but always uses "true" and "false" on output.
 */
@ServiceProvider(service = PropertyConverter.class)
public final class BooleanPropertyConverter implements PropertyConverter<Boolean>
{

    @Override
    public Class<Boolean> getType()
    {
        return Boolean.class;
    }

    @Override
    public Boolean fromString(String value)
    {
       return TextUtil.str2boolean(value);
    }

    @Override
    public String toString(Boolean value)
    {
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }
}
