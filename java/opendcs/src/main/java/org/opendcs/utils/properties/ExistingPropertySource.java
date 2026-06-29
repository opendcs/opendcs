package org.opendcs.utils.properties;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import ilex.util.HasProperties;

public class ExistingPropertySource<T> implements HasProperties
{
    private final Property<T> property;

    public ExistingPropertySource(Property<T> property)
    {
        this.property = property;
    }

    @Override
    public void setProperty(String name, String value)
    {
        /* do nothing */
    }

    @Override
    public String getProperty(String name)
    {
        if (property == null)
        {
            return null;
        }
        String ret = "" + property.defaultValue;
        for (var source: property.sources)
        {
            var tmp = source.getProperty(name);
            if (tmp != null)
            {
                ret = tmp;
                break;
            }
        }
        return ret;
    }

    @Override
    public Enumeration getPropertyNames()
    {
        return Collections.enumeration(List.of(property.name()));
    }

    @Override
    public void rmProperty(String name)
    {
        /* do nothing */
    }
}
