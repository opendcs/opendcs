/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2006/08/25 11:54:18  mmaloney
*  dev
*
*  Revision 1.5  2005/04/25 21:38:08  mjmaloney
*  dev
*
*  Revision 1.4  2004/08/24 21:01:37  mjmaloney
*  added javadocs
*
*  Revision 1.3  2004/04/15 19:47:48  mjmaloney
*  Added status methods to support the routng status monitor web app.
*
*  Revision 1.2  2003/06/06 01:39:20  mjmaloney
*  Datasources to handle either datasource or routingspec properties.
*  Consumers to handle delimiters consistently.
*  FileConsumer and DirectoryConsumer to handle File Name Templates.
*
*  Revision 1.1  2001/09/14 21:16:42  mike
*  dev
*
*/
package decodes.consumer;

import java.io.PrintStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import ilex.util.AsciiUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.db.*;

/**
  PipeConsumer sends data to the standard output, standard error, or a
  specified program.
  <p>
  The consumerArg may be blank, 'stdout', or 'stderr', or an executable program.
  The default (blank) means the same as stdout.
  Properties honored:
  <ul>
    <li>ConsumerBefore - string written before each message</li>
    <li>ConsumerAfter - string written after each message</li>
  </ul>
*/
public class PipeConsumer extends DataConsumer
{
	PrintStream os;
	Process childProc;
	private String before;
	private String after;

	/** default constructor */
	public PipeConsumer()
	{
		super();
		os = null;
		childProc = null;
		before = null;
		after = null;
	}

	/**
	  Opens and initializes the consumer. If a command was specified it
	  is executed and it's standard input is open.
	  @param consumerArg Should be blank, stdout, or stderr.
	  @param props routing spec properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		if (consumerArg == null
		 || consumerArg.equalsIgnoreCase("stdout")
		 || consumerArg.trim().length() == 0)
			os = System.out;
		else if (consumerArg.equalsIgnoreCase("stderr"))
			os = System.err;
		else
		{
			try
			{
				Logger.instance().log(Logger.E_INFORMATION,
					"Starting command '" + consumerArg + "'");
				childProc = Runtime.getRuntime().exec(consumerArg);
				os = new PrintStream(childProc.getOutputStream());
			}
			catch(Exception ex)
			{
				throw new DataConsumerException(
					"Cannot start child process '" + consumerArg + "'");
			}
		}

		// Use props for before & after strings
		String p = PropertiesUtil.getIgnoreCase(props, "ConsumerBefore");
		if (p != null)
			before = new String(AsciiUtil.ascii2bin(p));

		p = PropertiesUtil.getIgnoreCase(props, "ConsumerAfter");
		if (p != null)
			after = new String(AsciiUtil.ascii2bin(p));
	}

	/**
	  Closes the data consumer.
	  This method is called by the routing specification when the data
	  consumer is no longer needed.
	  If a child process was spawned, this kills it.
	*/
	public void close()
	{
		if (childProc != null)
		{
			try
			{
				childProc.destroy();
				childProc.waitFor();
				os.close();
			}
			catch(Exception ex)
			{
			}
			childProc = null;
		}
		os = null;
	}

	/**
	  Starts a new message, and attempts to reap any previously started child
	  process.
	  @param msg the message to start.
	  @throws DataConsumerException if an error occurs.
	*/
	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		if (childProc != null)
		{
			try
			{
				int i = childProc.exitValue();
				throw new DataConsumerException("Child process has exited.");
			}
			catch(IllegalThreadStateException ex) { /* still running */ }
		}
			
		if (before != null)
			os.print(before);
	}

	/**
	  Outputs a line.
	  @param line the line to output
	*/
	public void println(String line)
	{
		os.println(line);
	}

	/** Flush output and close the pipe. */
	public void endMessage()
	{
		if (after != null)
			os.print(after);
		os.flush();
	}

	/** @return "pipe" */
	public String getActiveOutput() { return "pipe"; }

	public OutputStream getOutputStream()
	{
		return os;
	}
	
	@Override
	public String getArgLabel()
	{
		return "Command";
	}

}

