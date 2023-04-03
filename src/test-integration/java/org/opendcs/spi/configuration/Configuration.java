package org.opendcs.spi.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.Extension;

/**
 * Baseline of a test implementation configuration
 */
public interface Configuration {
    public File getPropertiesFile();
    public File getUserDir();
    public boolean isSql();
    public default List<Extension> getExtensions() {
        return new ArrayList<>();
    }
}
