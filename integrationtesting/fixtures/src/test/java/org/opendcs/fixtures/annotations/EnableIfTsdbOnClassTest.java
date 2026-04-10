package org.opendcs.fixtures.annotations;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@EnableIfTsDb({"none"})
class EnableIfTsdbOnClassTest
{
    @BeforeEach
    void setup()
    {
        fail("I should not run");
    }

    @Test
    void should_not_run()
    {
        fail("I was not disabled.");
    }
}
