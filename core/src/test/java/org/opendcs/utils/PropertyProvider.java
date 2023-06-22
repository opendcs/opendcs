package org.opendcs.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
<<<<<<< HEAD:core/src/test/java/org/opendcs/utils/PropertyProvider.java
import java.io.IOException;

=======

import org.jooq.exception.IOException;
>>>>>>> 891e1381 (Initial generic value expander.):src/test/java/org/opendcs/utils/PropertyProvider.java
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fixtures.FileUtils;
<<<<<<< HEAD:core/src/test/java/org/opendcs/utils/PropertyProvider.java
import ilex.util.EnvExpander;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

public class PropertyProviderTest
{
    private static final String SECRET_FILE_VAR = "secret file value";
    private static final String ENV_VAR = "test env value";
    private static final String ENV_VAR_NAME ="THE_TEST_ENV_VAR";
    private static final String PROP_VAR = "test prop value";
    private static final String PROP_VAR_NAME ="test.provider.prop";
    static File secretFileProperty = null;

=======
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

public class PropertyProvider
{
    private static final String SECRET_FILE_VAR = "TEST_FILE_VAR";
    private static final String ENV_VAR = "TEST_ENV_VAR";
    private static final String ENV_VAR_NAME ="THE_TEST_ENV_VAR";
    private static final String PROP_VAR = "TEST_PROP_VAR";
    private static final String PROP_VAR_NAME ="test.provider.prop";
    static File secretFileProperty = null; 
    
>>>>>>> 891e1381 (Initial generic value expander.):src/test/java/org/opendcs/utils/PropertyProvider.java
    static EnvironmentVariables envVars = new EnvironmentVariables();

    @ParameterizedTest
    @CsvSource({
        "TEST_VALUE,TEST_VALUE",
        "\\${TEST_BRACKETS},\\${TEST_BRACKETS}",
        "\\${TEST_BRACKET_MISSING,\\${TEST_BRACKET_MISSING"
    })
    public void test_value_is_real_value_returned(String valueIn, String valueOut) throws Exception
    {
        assertEquals(valueOut,Property.getRealPropertyValue(valueIn));
    }

    @Test
    public void test_default_value_returned() throws Exception
    {
        final String value = "TEST_DEFAULT_VALUE";
        assertEquals(value,Property.getRealPropertyValue(null, value));
    }

<<<<<<< HEAD:core/src/test/java/org/opendcs/utils/PropertyProvider.java
=======
    @Test 
    void test_throws_when_def_not_right() throws Exception
    {
        assertThrows(IOException.class, () -> {
            String value = Property.getRealPropertyValue("${missing end curly");
        }, "Failed to process bad definition correctly");
    }

    @Test
    void test_throws_when_bad_provider() throws Exception
    {
        assertThrows(IOException.class, () -> {
            String value = Property.getRealPropertyValue("${non existent provider.I'm the real value}");
        }, "Failed to process bad definition correctly");
    }


>>>>>>> 891e1381 (Initial generic value expander.):src/test/java/org/opendcs/utils/PropertyProvider.java
    @BeforeAll
    static void setup_properties() throws Exception
    {
        secretFileProperty = File.createTempFile("secret_prop_test", null);
        secretFileProperty.deleteOnExit();
        FileUtils.writeFile(secretFileProperty,SECRET_FILE_VAR);
        System.setProperty(PROP_VAR_NAME,PROP_VAR);
        envVars.set(ENV_VAR_NAME,ENV_VAR);
    }

    @AfterAll
    static void clear_properties() throws Exception
    {
        System.clearProperty(PROP_VAR_NAME);
    }

    @Test
    public void test_default_providers() throws Exception
    {
<<<<<<< HEAD:core/src/test/java/org/opendcs/utils/PropertyProvider.java
        final String resultFile = Property.getRealPropertyValue("file."+secretFileProperty.getAbsolutePath());
        assertEquals(SECRET_FILE_VAR, resultFile, "Could not properly retrieve variable from file.");

        envVars.execute(() -> {
            final String envVar = Property.getRealPropertyValue("env." + ENV_VAR_NAME);
            assertEquals(ENV_VAR, envVar, "Could not properly retrieve variable from environment.");
        });

        final String propVar = Property.getRealPropertyValue("java." + PROP_VAR_NAME);
=======
        final String resultFile = Property.getRealPropertyValue("${file."+secretFileProperty.getAbsolutePath()+"}");
        assertEquals(SECRET_FILE_VAR, resultFile, "Could not properly retrieve variable from file.");

        final String envVar = Property.getRealPropertyValue("${env." + ENV_VAR_NAME + "}");
        assertEquals(ENV_VAR, envVar, "Could not properly retrieve variable from environment.");

        final String propVar = Property.getRealPropertyValue("${java." + PROP_VAR_NAME +"}");
>>>>>>> 891e1381 (Initial generic value expander.):src/test/java/org/opendcs/utils/PropertyProvider.java
        assertEquals(PROP_VAR, propVar,"Could not properly retrieve variable from java system properties.");

        final String passThroughExpected = "\\${test}";
        final String passThroughVar = Property.getRealPropertyValue(passThroughExpected);
        assertEquals(passThroughExpected, passThroughVar, "Passthrough did not work.");

    }
<<<<<<< HEAD:core/src/test/java/org/opendcs/utils/PropertyProvider.java


    @ParameterizedTest
    @CsvSource({
        "Hello ${env." + ENV_VAR_NAME + "}_${" + PROP_VAR_NAME + "},Hello test env value_test prop value"
    })
    public void test_expansion(String valueIn, String valueExpanded) throws Exception
    {
        envVars.execute(() -> {
            final String expansion = EnvExpander.expand(valueIn);
            assertEquals(valueExpanded,expansion, "Could not set all values.");
        });
    }
=======
>>>>>>> 891e1381 (Initial generic value expander.):src/test/java/org/opendcs/utils/PropertyProvider.java
}
