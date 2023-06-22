package org.opendcs.spi.properties;

import java.io.IOException;

public interface PropertyValueProvider {
    /**
     * Determine if a given string can be processed by this provider
     * @param value
     * @return
     */
    public boolean canProcess(String value);

    /**
     * Retrieve the value from it's actual location
     * @param value
     * @return
     */
    public String processValue(String value) throws IOException;
}
