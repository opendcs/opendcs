package org.opendcs.utils.properties.conversion;

public interface PropertyConverter<T>
{
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
}
