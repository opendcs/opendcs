package org.opendcs.fixtures.spi;

import java.io.File;

public interface ConfigurationProvider {
    /**
     * Unique name of this implementation
     * @return name
     */
    public String getImplementation();

    /**
     * After any required setup all required information for an "install" to
     * operate
     *
     * @param tempDir temporary location to expand and build "opendcs_userdir"
     *                DCSTOOL_USERDIR will point to this location.
     * @return Configuration instance 
     * @throws Exception if an error occurs during configuration setup.
     */
    public Configuration getConfig(File tempDir) throws Exception;
}
