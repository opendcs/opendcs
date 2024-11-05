package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class ClasspathIOTest {
    @Test
    public void test_all_files_retrieved() throws Exception
    {
        List<URL> resources = ClasspathIO.getAllResourcesIn("decodes");
        assertFalse(resources.isEmpty(), "No resources were loaded.");
    }
}
