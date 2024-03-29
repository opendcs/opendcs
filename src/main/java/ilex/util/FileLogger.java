/*
*  $Id$
*/
package ilex.util;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.*;
import java.nio.channels.FileChannel;


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
public class FileLogger extends Logger
{
	/**
	* The current output PrintStream
	*/
	private PrintStream output = null;

	/**
	* The filename supplied to the constructor.
	*/
	protected String filename = null;

	/**
	* File object constructed from the file name.
	*/
	protected File outputFile = null;

	/**
	* Default length if none supplied by user.
	*/
	private static int defaultMaxLength = 10000000;  // 10 meg.

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

	private FileChannel fileChan = null;
	private AtomicBoolean closeOperations = new AtomicBoolean(false);	
	private BlockingQueue<String> queue = new ArrayBlockingQueue<>(5000); // TODO: intentionally arbitrary number for now.;
	private Thread writerThread;
	
	/**
	* Construct with a process name and a filename.
	* @param procName Name of this process, to appear in each log message.
	* @param filename Name of log file.
	* @throws FileNotFoundException if can't open file
	*/
	public FileLogger( String procName, String filename )
		throws FileNotFoundException
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
	* @throws FileNotFoundException if can't open file
	*/
	public FileLogger( String procName, String filename, int maxLength )
		throws FileNotFoundException
	{
		super(procName);
		this.filename = EnvExpander.expand(filename);
		this.maxLength = maxLength;
//		outputFile = new File(EnvExpander.expand(filename));
		openNewLog();
		
//		FileOutputStream fos = new FileOutputStream(outputFile, appendFlag);
//		output = new PrintStream(fos, true);
	}

	/**
	* Close this log file.
	*/
	public void close( )
	{
		closeOperations.set(true);
		if (output != null)
		{
			output.close();
		}
		fileChan = null;
		output = null;
		
	}

	/**
	* Logs a message.  The priority has already been checked to make sure
	* that this message should be logged.
	* This method is called from the base class log method.
	* @param priority the priority
	* @param text the formatted log message text
	*/
	public synchronized void doLog( int priority, String text )
	{
		queue.add(standardMessage(priority, text));
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
		close();
		renameCurrentLog();
		openNewLog();
	}

	protected void openNewLog()
	{
		outputFile = new File(filename);
		try
		{
			FileOutputStream fos = new FileOutputStream(outputFile, appendFlag);
			output = new PrintStream(fos, true);
			fileChan = fos.getChannel();
			writerThread = new Thread(() ->
			{
				while(closeOperations.get() == false)
				{
					try
					{
						String msg = queue.poll(1, TimeUnit.SECONDS);
						if (msg != null && output != null && !output.checkError())
						{
							output.println(msg);
						}
					}
					catch (InterruptedException ex)
					{

					}
				}
			},
			"FileLogger-Writer");
			writerThread.start();
		}
		catch(IOException ex)
		{
			System.err.println("IOException trying to open log file '" 
				+ filename + "': " + ex);
			throw new RuntimeException("Unable to open log file.",ex);
		}
	}

	/**
	 * Renames the current log with a suffix indicating it is now old.
	 */
	protected void renameCurrentLog()
	{
		File oldFile = new File(filename + ".old");
		if (oldFile.exists())
			oldFile.delete();
		outputFile.renameTo(oldFile);
	}

	/**
	* Returns the PrintStream currently being used for log output. Allows
	* caller to print directly to the file. E.g. for exception stack traces.
	* @return PrintStream for direct log file output
	*/
	public PrintStream getLogOutput( )
	{
		return output;
	}

	/**
	* Sets the maximum length of the file. When the file reaches this length
	* it is renamed with a ".old" extension, and a new file is created.
	* The software appends log messages until the specified length has
	* been exceeded. Hence the actual length of the file may be slightly
	* more than the specified maximum.
	* 
	* @param len Length of file in bytes.
	*/
	public void setMaxLength( int len )
	{
		maxLength = len;
	}
}
