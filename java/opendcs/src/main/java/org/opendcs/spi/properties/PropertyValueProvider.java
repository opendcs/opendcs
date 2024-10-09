package org.opendcs.spi.properties;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public interface PropertyValueProvider {
    /**
     * Determine if a given string can be processed by this provider
     * @param value
     * @return
     */
    public boolean canProcess(String value);

    /**
     * Retrieve property from the provided property or environment map.
     *
     * It is permissible for a given implemtation to completely ignore either the properties or
     * environment map. However, it should be made very clear where data is coming from
     *
     * @param value actual value to decipher.
     *
     * @param properties Properties to use for the given request.
     * @param environment Environment map to use for the given request.
     *
     * @return the real value, or null if not found.
     */
    public String processValue(String value, Properties properties, Map<String,String> env) throws IOException;
}
