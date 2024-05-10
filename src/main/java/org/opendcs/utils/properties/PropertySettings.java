package org.opendcs.utils.properties;

public class PropertySettings {
    
    /**
     * Allows the user to see where a property wasn't picked up correctly 
     * while diagnosing using external sources for property values.
     * Turn this on sparingly as it generates a lot of noise for properties that 
     * Don't reference an external source but just have a raw value.
     * @since 7.0.14
     */
    public static final boolean TRACE_PROPERTY_PROVIDERS = 
        Boolean.parseBoolean(
                System.getProperty("opendcs.property.providers.trace", "false")
            );

    private PropertySettings() {}
}
