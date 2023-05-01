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
    Stream<DynamicNode> opendcsCompatibilityKit() throws IOException
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

    private DynamicContainer testsFor(OpenDCSAppTestCase openDCSAppTestCase)
    {
        return dynamicContainer(openDCSAppTestCase.getConfigurationName(),Stream.of(
            dynamicTest("Config not null", ()-> assertNotNull(openDCSAppTestCase,"Test Case definition can't be null.")),
            new DecodesTest(openDCSAppTestCase).getTests()
        ));
    }
}
