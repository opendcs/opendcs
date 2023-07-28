/*
*  $Id$
*/
package ilex.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import org.opendcs.logging.IlexToSlf4jBridge;

import static org.opendcs.logging.JULHelpers.ilexLevelToJulLevel;

/**
* Concrete subclass of Logger that Logs messages to a file.
* This provides a facility to limit the size of the log file to some
* set maximum.  The default maximum size of a log file is 10 megabytes.
* <p>
* If a log file with the given name already exists, new messages are
* appended to the end of it.  When the log file reaches the maximum
* length, it will be closed, ".old" will be appended to its name, and
* then a new log file will be opened with the original name.
* </p>
*/
public class FileLogger extends IlexToSlf4jBridge
{
    /**
    * The filename supplied to the constructor.
    */
    protected String filename = null;

    /**
     * JUL Handler
     */
    protected Handler handler = null;
    /**
    * Default length if none supplied by user.
    */
    private static final int defaultMaxLength = 10000000;  // 10 meg.

    /**
    * User-settable maximum log file length.
    */
    private int maxLength = defaultMaxLength;

    /**
    * Flag determining whether constructor overwrites or appends.
    * By default, set to true, meaning that the constructor will append
    * to an existing file when it starts up.
    * Set to false to cause the constructor to zero and start a new log.
    */
    public static boolean appendFlag = true;    

    /**
    * Construct with a process name and a filename.
    * @param procName Name of this process, to appear in each log message.
    * @param filename Name of log file.
    * @throws FileNotFoundException if can't open file
    */
    public FileLogger( String procName, String filename )
        throws IOException
    {
        this(procName, filename, defaultMaxLength);
    }

    /**
    * Construct with a process name, a filename, and a maximum length.
    * When it reaches the maximum length, the log file will be closed,
    * ".old" will be appended to its name, and then a new log file will
    * be opened with the original name.
    * @param procName Name of this process, to appear in each log message.
    * @param filename Name of log file.
    * @param maxLength Maximum length of log file
    * @throws IOException if can't open file
    */
    public FileLogger( String procName, String filename, int maxLength)
        throws IOException
    {
        this(procName,filename,maxLength,2);
    }

    public FileLogger( String procName, String filename, int maxLength, int count)
        throws IOException
    {
        super(procName);
        this.filename = EnvExpander.expand(filename);
        this.maxLength = maxLength;

        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        if ( filename.trim().equalsIgnoreCase("/dev/stdout")
          || filename.trim().equalsIgnoreCase("-"))
        {
            handler = new ConsoleHandler();
            for(Handler h: root.getHandlers())
            {
                if (h instanceof ConsoleHandler)
                {
                    root.removeHandler(h);
                }
            }
        }
        else
        {
            handler = new FileHandler(filename+".%g", maxLength, count);
        }
        handler.setLevel(ilexLevelToJulLevel(this.minLogPriority));
        root.addHandler(handler);
    }

    /**
    * Close this log file.
    */
    public void close( )
    {        
    }    

    /**
     * Closes the current log, renames it with an aging extension, and
     * opens a new log.
     */
    public synchronized void rotateLogs()
    {
        // Note: This needs to be a separate method because it must be
        // synchronized with doLog().
        // And because doLog() calls rotate(), it can't be syncrhonized.
        rotate();
    }

    /**
     * Does the actual rotation.
     * Called internally when the log reaches its maximum size. May also
     * be called by the synchronized rotateLogs() method which can be
     * called externally by the application.
     */
    protected void rotate()
    {
        // handled by JUL Handler
    }

    protected void openNewLog()
    {
        // handled by JUL Handler
    }

    /**
     * Renames the current log with a suffix indicating it is now old.
     */
    protected void renameCurrentLog()
    {
        // handled by JUL Handler
    }

    /**
    * Returns the PrintStream currently being used for log output. Allows
    * caller to print directly to the file. E.g. for exception stack traces.
    * @return PrintStream for direct log file output
    */
    public PrintStream getLogOutput( )
    {
        return null;
    }
}
