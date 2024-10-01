package ilex.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.slf4j.LoggerFactory;

/**
You have to be careful when spawning processes from within Java to process
the stdout and stderr. Otherwise the sub-process could hang waiting for
IO to complete.
<p>
This class is a thread that waits for a background process to complete.
Any output from the process is converted to log messages.
</p>
*/
public class ProcWaiterThread extends Thread
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ProcWaiterThread.class);
    /**
    * The Process I'm waiting for
    */
    private Process proc;

    /**
    * Name used for log messages
    */
    private String name;

    /**
     * Optional callback.
     */
    private ProcWaiterCallback callback;

    /**
     * Optional object to pass back to the callback.
     */
    private Object callbackObj;

    public String cmdOutput = null;


    /**
    * Execute a command in the background, starting a ProcWaiterThread to
    * wait for the process and convert any output to log messages.
    * @param cmd the command
    * @param name the name for log messages
    * @throws IOException if the command could not be executed.
    */
    public static void runBackground( String cmd, String name ) throws IOException
    {
        log.debug("Executing '{}' in background", cmd);
        Process proc = Runtime.getRuntime().exec(cmd);
        ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
        pwt.start();
    }

    /**
     * Runs a command in the foreground and returns the output as a string.
     * @param cmd
     * @param name
     * @return
     * @throws IOException
     */
    public static String runForeground(String cmd, String name) throws IOException
    {
        log.debug("Executing '{}' in foreground", cmd);
        Process proc = Runtime.getRuntime().exec(cmd);
        ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
        pwt.run();
        return pwt.cmdOutput;
    }

    /**
    * Execute a command in the background, starting a ProcWaiterThread to
    * wait for the process and convert any output to log messages.
    * @param cmd the command
    * @param name the name for log messages
    * @param callback object to notify when process exits.
    * @param callbackObj opaque object to pass to the callback.
    * @throws IOException if the command could not be executed.
    */
    public static void runBackground( String cmd, String name, ProcWaiterCallback callback, Object callbackObj) throws IOException
    {
        log.debug("Executing '{}'", cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd.split("\\s+"));
        Map<String,String> env = pb.environment();
        // TODO: remove or expand; currently for debuging debug agent gets passed in when it shouldn't.
        env.remove("DECJ_MAXHEAP");
        Process proc = pb.start();
        ProcWaiterThread pwt = new ProcWaiterThread(proc, name);
        pwt.setCallback(callback, callbackObj);
        pwt.start();
    }

    /**
    * Construct a ProcWaiterThread for a particular process.
    * @param proc the process
    * @param name the name for log messages
    */
    private ProcWaiterThread(Process proc, String name)
    {
        this.proc = proc;
        this.name = name;
        callback = null;
        callbackObj = null;
    }

    /**
     * Sets the optional callback.
     */
    public void setCallback(ProcWaiterCallback callback, Object callbackObj)
    {
        this.callback = callback;
        this.callbackObj = callbackObj;
    }

    /**
    * Public run method
    */
    public void run()
    {
        // Start a separate thread to read the input stream.
        final InputStream is = proc.getInputStream();
        final org.slf4j.Logger processLogger = LoggerFactory.getLogger(ProcWaiterThread.class.getName()+"::cmd." + name);
        Thread isr =
                new Thread(() ->
                {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
                    {
                        String line = null;
                        while((line = reader.readLine()) != null)
                        {
                            cmdOutput = line;
                            processLogger.trace(line);
                        }
                    }
                    catch(IOException ex)
                    {
                        processLogger.atError()
                                     .setCause(ex)
                                     .log("{} error on output stream.", name);
                    }
                });
        isr.setDaemon(true);
        isr.setPriority(MIN_PRIORITY);
        isr.start();


        // Likewise for the stderr stream
        final InputStream es = proc.getErrorStream();
        Thread esr =
                new Thread(() ->
                {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(es)))
                    {
                        String line = null;
                        while((line = reader.readLine()) != null)
                        {
                            processLogger.error(line);
                        }
                    }
                    catch(IOException ex)
                    {
                        processLogger.atError()
                                     .setCause(ex)
                                     .log("Error on error stream.");
                    }
                });
        esr.setDaemon(true);
        esr.setPriority(MIN_PRIORITY);
        esr.start();

        // Finally, wait for process and catch its exit code.
        try
        {
            int exitStatus = proc.waitFor();
            // Race-condition, after process ends, wait a half sec for
            // reads in isr & esr above to finish.
            sleep(500L);
            if (exitStatus != 0)
            {
                processLogger.info("{} finished with exit code {}", name, exitStatus);
            }
            if (callback != null)
            {
                callback.procFinished(name, callbackObj, exitStatus);
            }
        }
        catch(InterruptedException ex)
        {
            processLogger.atError()
                         .setCause(ex)
                         .log("Interrupted waiting for {} to finished.", name);
        }
    }

    /**
    * Test main. Usage: java ilex.util.ProcWaiterThread <cmd> <name>
    * @param args  the args
    * @throws Exception on any error, printing stack trace.
    */
    public static void main( String[] args ) throws Exception
    {
        System.out.println("Executing '" + args[0] + "' output: ");
        System.out.println(runForeground(args[0], args[1]));
    }
}
