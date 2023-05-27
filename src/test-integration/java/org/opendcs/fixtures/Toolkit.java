package org.opendcs.fixtures;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.opendcs.regression_tests.DecodesTest;
import org.opendcs.spi.configuration.ConfigurationProvider;

public class Toolkit
{
    @TestFactory
    Stream<DynamicNode> opendcsCompatibilityKit() throws Exception
    {
        ArrayList<DynamicNode> tests = new ArrayList<>();
        ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
        Iterator<ConfigurationProvider> configs = loader.iterator();
        while(configs.hasNext())
        {
            ConfigurationProvider config = configs.next();
            File tmp = Files.createTempDirectory("configs").toFile();
            tests.add(testsFor(config.getConfig(tmp)));
            
        }
        return tests.stream();
    }

    private DynamicContainer testsFor(OpenDCSAppTestCase testCase) throws Exception
    {
        System.err.println("Setting up tests for: " + testCase.getDisplayName());
        return dynamicContainer(testCase.getDisplayName(),Stream.of(
            dynamicTest(testName(testCase,null,":Config not null"), ()-> assertNotNull(testCase,"Test Case definition can't be null.")),
            new DecodesTest(testCase).getTests(testCase.getDisplayName())
        ));
    }

    public static String testName(OpenDCSAppTestCase testCase, AppTestBase base, String method)
    {
        return testCase.getDisplayName()
              + ":"
              + (base != null ? base.getClass().getSimpleName() : "Toolkit")
              + ":"
              + method;
    }

    public static String testName(String base, String ...part)
    {
        StringBuilder sb = new StringBuilder(base);
        for (String p: part)
        {
            sb.append(":").append(p);
        }
        return sb.toString();
    }
}
