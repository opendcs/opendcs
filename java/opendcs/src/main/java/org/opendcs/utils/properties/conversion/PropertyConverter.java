package org.opendcs.utils.properties.conversion;

import java.util.ServiceLoader;

public interface PropertyConverter<T>
{

    Class<T> getType();

    /**
     * Given a string value of a property, converter to the required Type.
     * @param value
     * @return
     */
    T fromString(String value);

    /**
     * Given the property type, convert to appropriate string for storage.
     * @param value
     * @return
     */
    String toString(T value);


    @SuppressWarnings("unchecked") // type is specifically checked manually.
    static <T> PropertyConverter<T> forType(Class<T> type)
    {
        PropertyConverter<T> ret = null;
        final var converters = ServiceLoader.load(PropertyConverter.class);
        for (var converter: converters)
        {
            if (converter.getType().equals(type))
            {
                ret = converter;
                break;
            }
        }

        return ret;
    }
}
