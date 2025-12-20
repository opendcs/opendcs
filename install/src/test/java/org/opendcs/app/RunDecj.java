package org.opendcs.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RunDecj
{
    private static final ExecutorService exec = Executors.newFixedThreadPool(2);

    /**
     * Calls decj, or decj.bat on windows, using {@see org.opendcs.app.ArgumentEchoApp} and returns the text of std out, std err
     * and from stdout loads a java properties object based on the output provided by the application.
     * @param args
     * @return A valid Result which includes the arg list as seen by the application and the properties printed to std out.
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static Result appOutput(String ...args) throws IOException, InterruptedException, ExecutionException
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
        Future<String> stdOutput = exec.submit(() -> streamToString(procOutput));
        Future<String> stdError = exec.submit(() -> streamToString(procError));
        
        int code = proc.waitFor();
        final String stdErr = stdError.get();
        final String stdOut = stdOutput.get();
        proc.destroyForcibly(); // make sure it has really stopped.
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

    private static String streamToString(InputStream in) throws IOException
    {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream())
        {
            byte[] data = new byte[2048];
            int bytesRead = 0;
            while ((bytesRead = in.read(data, 0, data.length)) != -1)
            {
                buffer.write(data, 0, bytesRead);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
        
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
