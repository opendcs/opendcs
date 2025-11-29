package org.opendcs.fixtures.annotations;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableIfTsDb
public class EnableIfTsdbOnClassNoValueTest
{
    @BeforeEach
    void setup()
    {
        assertTrue(true, "test setup did not run");
    }

    @Test
    void should_not_run()
    {
        assertTrue(true, "test method did not run");
    }
}
