package org.opendcs.spi.configuration;

import java.io.File;

import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

public interface ConfigurationProvider {
    /**
     * Unique name of this implementation
     * @return
     */
    public String getImplementation();

    /**
     * After any required setup all required information for an "install" to 
     * operate
     * 
     * @param tempDir temporary location to expand and build "opendcs_userdir"
     *                DCSTOOL_USERDIR will point to this location.
     * @return
     */
    public TestTemplateInvocationContext getConfig(File tempDir);
}
