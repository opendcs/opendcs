package org.opendcs.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opendcs.app.RunDecj.Result;

import static org.opendcs.app.RunDecj.appOutput;

class DecjTest 
{
    


    @Test
    void test_set_log() throws Exception
    {
        final Result output = appOutput("-l", "test.log");
        assertEquals(Collections.EMPTY_LIST, output.args,
                     () -> "argument not consumed. cmd was: '" +
                           output.props.getProperty("cli", "could not retrieve CLI value") + "'");
        assertEquals("test.log",output.props.getProperty("LOG_FILE",""));
    }

    @ParameterizedTest
    @ArgsSource({"-l","test.log"})
    @ArgsSource({"-l","test.log", "-Dtest=test"})
    @ArgsSource({"-l","test.log", "-Dtest=test", "-DLRGSHOME=testdir"})
    @ArgsSource({"-ltest.log"})
    @ArgsSource({"-ltest.log", "-d3"})
    @ArgsSource({"-ltest.log", "-d", "3"})
    @ArgsSource({"-DLRGSHOME=testdir", "-l", "test.log", "-Dtest=test"})
    void test_arguments_consumed(String[] args) throws Exception
    {
        final Result output = appOutput(args);
        assertEquals(Collections.EMPTY_LIST, output.args,
                     () -> "argument not consumed. cmd was: '" +
                           output.props.getProperty("cli", "could not retrieve CLI value") + "'");
        assertEquals("test.log",output.props.getProperty("LOG_FILE",""));
    }

    @ParameterizedTest
    @ArgsSource({"-l","test.log", "-a", "test"})
    @ArgsSource({"-ltest.log","-a", "test"})
    @ArgsSource({"-ltest.log","-d3","-a", "test"})
    @ArgsSource({"-a", "test", "-ltest.log","-d3","-a"})
    @ArgsSource({"-ltest.log","-d", "3","-a", "test"})
    void test_arguments_not_consumed(String[] args) throws Exception
    {
        final List<String> expected = new ArrayList<>();
        expected.add("-a");
        expected.add("test");
        final Result output = appOutput(args);
        assertEquals(expected, output.args,
                     () -> "argument properly not consumed. cmd was: '" +
                           output.props.getProperty("cli", "could not retrieve CLI value") + "'");
    }


    @ParameterizedTest
    @ArgsSource({"-S", "now - 1 hour"})
    void test_arguments_with_spaces(String[] args) throws Exception
    {
        final Result output = appOutput(args);
        assertEquals(args.length,
                     output.args.size(),
                     () -> "In correct number of arguments provided to application" +
                           "\ncli was " + output.props.getProperty("cli", "CLI not created") +
                           "\n args was " + output.props.getProperty("args", "no args?") +
                           "\n stdout was:\n" + output.standardOut +
                           "\n stderr was:\n" + output.errorOut);
    }

    
}
