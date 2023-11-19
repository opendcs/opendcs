package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ClasspathIOTest {
    @Test
    public void test_all_files_retrieved() throws Exception
    {
        List<URL> resources = ClasspathIO.getAllResourcesIn("decodes");
        assertFalse(resources.isEmpty(), "No resources were loaded.");
    }

    @Test
    public void test_classpath_url_handler() throws Exception
    {
        URL url = new URL("classpath:///hello.txt");
        assertNotNull(url);
        byte buffer[] = new byte[5];
        url.openStream().read(buffer);
        String msg = new String(buffer,StandardCharsets.UTF_8);
        assertEquals(msg,"hello");
    }
}
