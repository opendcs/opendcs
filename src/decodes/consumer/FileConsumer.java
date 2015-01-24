/*
*  $Id$
*/
package decodes.consumer;

import java.io.PrintStream;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Properties;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.TextUtil;

import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.db.*;

/**
FileConsumer sends data to a named file.
The conumerArg specifies the file name template, which may contain
substitution variables.
<p>Properties used by FileConsumer include:</p>
<ul>
  <li>file.overwrite - true/false flag indicating whether file should be
      overwritten (default) or appended.</li>
  <li>ConsumerBefore - String written at the beginning of each message</li>
  <li>ConsumerAfter - String written at the end of each message</li>
  <li>cmdAfterFile - Optional command line to execute after closing file.</li>
  <li>cmdTimeout - # seconds to wait for cmdAfterFile to complete (default 
      60)</li>
</ul>
*/
public class FileConsumer extends DataConsumer
	implements ProcWaiterCallback
{
	/** Used to write to the file. */
	private PrintStream os;
	/** delimiter written before each message */
	private byte[] before;
	/** delimiter written after each message */
	private byte[] after;
	/** complete filename after substitutions */
	private String filename;

	/* Variables to implement 'cmdAfterFile' functionality: */
	private String cmdAfterFile;
	private int cmdTimeout;
	private boolean cmdFinished;
	private int cmdExitStatus;
	private String cmdInProgress;
	private Properties props;


	/** 
	  In the absense of a file.overwrite property, this defines the default
	  behavior.
	*/
	public static boolean defaultFileOverwrite = true;

	/** default constructor */
	public FileConsumer()
	{
		super();
		os = null;
		before = null;
		after = null;
		filename = null;
		cmdAfterFile = null;
		cmdTimeout = 60;
		cmdFinished = false;
		cmdExitStatus = 0;
		cmdInProgress = null;
	}

	/**
	  Opens and initializes the consumer.
	  @param consumerArg file name template.
	  @param props routing spec properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		this.props = props;
		String fn = EnvExpander.expand(consumerArg, props);
		this.filename = fn;
		try
		{
			// open file named in consumerArg
			Logger.instance().info("Opening '" + fn + "'");
			File f = new File(fn);
			boolean overwrite = defaultFileOverwrite;
			String s = props.getProperty("file.overwrite");
			if (!f.exists())
				overwrite = true;
			else if (s != null)
				overwrite = TextUtil.str2boolean(s);
			
			FileOutputStream fos = new FileOutputStream(f, !overwrite);
			os = new PrintStream(fos);
		}
		catch(IOException e)
		{
			throw new DataConsumerException("Cannot open file '"
				+ consumerArg + "': " + e);
		}

		// Use props for before & after strings
		String p = props.getProperty("ConsumerBefore");
		if (p != null)
			before = AsciiUtil.ascii2bin(p);

		p = props.getProperty("ConsumerAfter");
		if (p != null)
			after = AsciiUtil.ascii2bin(p);

		String s = PropertiesUtil.getIgnoreCase(props, "cmdAfterFile");
		if (s != null && s.trim().length() > 0)
			cmdAfterFile = s;
	}

	/**
	  Closes the data consumer.
	  This method is called by the routing specification when the data
	  consumer is no longer needed.
	*/
	public void close()
	{
		if (os != null)
			os.close();

		if (cmdAfterFile != null)
		{
			Properties cmdProps = new Properties();
			PropertiesUtil.copyProps(cmdProps, props);
			PropertiesUtil.rmIgnoreCase(cmdProps, "FILENAME");
			cmdProps.setProperty("FILENAME", filename);
			cmdInProgress = EnvExpander.expand(cmdAfterFile, cmdProps);
			Logger.instance().debug1("Executing '" + cmdInProgress 
				+ "' and waiting up to " + cmdTimeout 
				+ " seconds for completion.");
			cmdFinished = false;
			try 
			{
				cmdExitStatus = -1;
				ProcWaiterThread.runBackground(cmdInProgress, 
					"post-file-cmd", this, cmdInProgress);
			}
			catch(IOException ex)
			{
				Logger.instance().warning("Cannot execute '" 
					+ cmdInProgress + "': " + ex);
				cmdInProgress = null;
				cmdFinished = true;
				return;
			}
			long startMsec = System.currentTimeMillis();
			while(!cmdFinished
			 && (System.currentTimeMillis()-startMsec) / 1000L < cmdTimeout)
			{
				try { Thread.sleep(1000L); }
				catch(InterruptedException ex) {}
			}
			if (cmdFinished)
				Logger.instance().debug1("Command '" + cmdInProgress 
					+ "' completed with exit status " + cmdExitStatus);
			else
				Logger.instance().warning("Command '" + cmdInProgress 
					+ "' Did not complete!");
		}
		filename = null;
	}

	/**
	  @param msg The message about to be written.
	  @throws DataConsumerException if an error occurs.
	*/
	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		try
		{
			if (before != null)
				os.write(before);
		}
		catch(IOException e) {}
	}

	/*
	  Sends a line of text to the file.
	  @param line the line to be written.
	*/
	public void println(String line)
	{
		os.println(line);
	}

	/**
	  Called after all data is written, flushes the output and closes the file.
	*/
	public void endMessage()
	{
		try
		{
			if (after != null)
				os.write(after);
			os.flush();
		}
		catch(IOException e) {}
	}

	/** @return current file name. */
	public String getActiveOutput()
	{
		return filename != null ? filename : "(none)";
	}

	public OutputStream getOutputStream()
	{
		return os;
	}

	public void procFinished(String procName, Object obj, int exitStatus)
	{
		if (obj != cmdInProgress)
			return;
		cmdFinished = true;
		cmdExitStatus = exitStatus;
	}

	@Override
	public String getArgLabel()
	{
		return "File Name";
	}

}

