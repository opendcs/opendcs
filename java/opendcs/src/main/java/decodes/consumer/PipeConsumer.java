/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.consumer;

import java.io.PrintStream;
import java.io.OutputStream;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.AsciiUtil;
import ilex.util.PropertiesUtil;

import decodes.decoder.DecodedMessage;
import decodes.util.PropertySpec;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	PrintStream os;
	Process childProc;
	private String before;
	private String after;

	private PropertySpec[] myspecs = new PropertySpec[]
	{
		new PropertySpec("ConsumerBefore", PropertySpec.STRING,
			"Optional string placed at the start of each message."),
		new PropertySpec("ConsumerAfter", PropertySpec.STRING,
			"Optional string placed at the end of each message.")
	};

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
				log.info("Starting command '{}'", consumerArg);
				childProc = Runtime.getRuntime().exec(consumerArg);
				os = new PrintStream(childProc.getOutputStream());
			}
			catch (Exception ex)
			{
				throw new DataConsumerException("Cannot start child process '" + consumerArg + "'", ex);
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

	public void setOutputStream(PrintStream os)
	{
		this.os = os;
	}

	@Override
	public String getArgLabel()
	{
		return "Command";
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myspecs;
	}
}
