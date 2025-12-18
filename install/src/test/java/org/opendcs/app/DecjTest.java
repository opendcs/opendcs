package org.opendcs.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

class DecjTest 
{
    private Result appOutput(String ...args) throws IOException, InterruptedException
    {
        List<String> argsList = new ArrayList<>();
        String script = "build/install/opendcs/bin/decj";
        if (System.getProperty("os.name").toLowerCase().contains("win"))
        {
            script += ".bat";
        }
        else
        {
            argsList.add("bash");
            argsList.add("-x");
        }
        argsList.add(script);
        argsList.add("org.opendcs.app.ArgumentEchoApp");
        for (String arg: args)
        {
            argsList.add(arg);    
        }
        ProcessBuilder pb = new ProcessBuilder(argsList.toArray(new String[0]));
        pb.environment().putAll(System.getenv());
        Process proc = pb.start();
        Properties props = new Properties();
        InputStream procOutput = proc.getInputStream();
        InputStream procError = proc.getErrorStream();
        try (ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
             ByteArrayOutputStream outBuffer = new ByteArrayOutputStream())
        {
            byte[] data = new byte[2048];
            int bytesRead = 0;
            while ((bytesRead = procError.read(data, 0, data.length)) != -1)
            {
                errorBuffer.write(data, 0, bytesRead);
            }
            bytesRead = 0;
            while ((bytesRead = procOutput.read(data, 0, data.length)) != -1)
            {
                outBuffer.write(data, 0, bytesRead);
            }
            int code = proc.waitFor();
            final String stdErr = new String(errorBuffer.toByteArray());
            final String stdOut = new String(outBuffer.toByteArray());
            props.load(new StringReader(stdOut));
            assertEquals(0, code, () -> "Unexpected exit stderr: " + stdErr +
                                                  "\n stdout: "+ stdOut +
                                                 "\nCommand was " + String.join(" ", pb.command()) +
                                                " \nCli was " + props.getProperty("cli"));

            List<String> argsListOut = new ArrayList<>();
            String theArgs = props.getProperty("args","").trim();
            theArgs = theArgs.trim();
            if (!theArgs.isEmpty())
            {
                String[] argsArray = theArgs.split(",");
                for (String arg: argsArray)
                {
                    argsListOut.add(arg.trim());
                }
            }
            return new Result(argsListOut, props, stdOut, stdErr);
        }
        
    }


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
    @ArgsSource({"-ltest.log"})
    @ArgsSource({"-ltest.log","-d3"})
    @ArgsSource({"-ltest.log","-d", "3"})
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

    public static class Result
    {
        final List<String> args;
        final Properties props;
        final String errorOut;
        final String standardOut;

        public Result(List<String> args, Properties props, String standardOut, String errorOut)
        {
            this.args = args;
            this.props = props;
            this.errorOut = errorOut;
            this.standardOut = standardOut;
        }
    }
}
