package org.opendcs.utils.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class OpenDcsLoggerFactoryTest
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Test
    void test_logger_name_correctly_retrieved_static()
    {
        assertEquals(OpenDcsLoggerFactoryTest.class.getName(), log.getName(), "Incorrect logger name retrieved.");
    }

    @Test
    void test_logger_name_correctly_retrieved()
    {
        final Logger log1 = OpenDcsLoggerFactory.getLogger();
        assertEquals(OpenDcsLoggerFactoryTest.class.getName(), log1.getName(), "Incorrect logger name retrieved.");

        final String logName = "a.test.logger";
        final Logger log2 = OpenDcsLoggerFactory.getLogger(logName);
        assertEquals(logName, log2.getName());

        final Class<?> testClass = OpenDcsLoggerFactoryTest.class;
        final Logger log3 = OpenDcsLoggerFactory.getLogger(testClass);
        assertEquals(testClass.getName(), log3.getName());


        NestedTestClass testInst = new NestedTestClass();
        assertEquals(NestedTestClass.class.getName(), testInst.getLogName());
    }

    public static class NestedTestClass {
        private static Logger log = OpenDcsLoggerFactory.getLogger();

        public String getLogName()
        {
            return log.getName();
        }
    }
}
