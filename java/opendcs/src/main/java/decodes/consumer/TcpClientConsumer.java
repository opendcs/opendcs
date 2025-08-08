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

import ilex.net.BasicClient;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;


import java.io.OutputStream;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.decoder.DecodedMessage;
import decodes.util.PropertySpec;

/**
 * Implements the client-side of a TCP socket for consuming data from DECODES.
 * @author mmaloney
 *
 */
public class TcpClientConsumer extends DataConsumer
{
	private static Logger log = OpenDcsLoggerFactory.getLogger();
	public static String module="TcpClientConsumer";
	private boolean connectPerMessage = false;
	private String before = null;
	private String after = null;
	private String hostname = null;
	private int port = -1;
    private BasicClient con = null;
    private int connectTries = 3;
    private int connectPauseSec = 60;
    private static String lineSep = System.getProperty("line.separator");

	private PropertySpec propSpecs[] =
	{
		new PropertySpec("connectPerMessage", PropertySpec.BOOLEAN,
			"If true, establish separate socket for each message (default=false)"),
		new PropertySpec("before", PropertySpec.STRING,
			"Optional prefix written before the start of each message."),
		new PropertySpec("after", PropertySpec.STRING,
			"Optional suffixe written after the end of each message."),
		new PropertySpec("connectTries", PropertySpec.INT,
			"Number of times to try to connect (default=3)"),
		new PropertySpec("connectPauseSec", PropertySpec.INT,
			"Number of seconds to wait between connect attempts (default=60)")
	};

	/** No args constructor used by Java reflection */
	public TcpClientConsumer()
	{
		super();
	}


	@Override
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		String hp = consumerArg;
		if (hp == null || (hp = hp.trim()).length() == 0)
			throw new DataConsumerException("Missing required 'host:port' argument.");

		int idx = hp.indexOf(':');
		if (idx < 0)
			idx = hp.indexOf(' ');
		if (idx <= 0 || idx == hp.length()-1)
			throw new DataConsumerException("Invalid 'host:port' argument '" + hp + "'");

		hostname = hp.substring(0, idx);
		String ps = hp.substring(idx+1);
		try { port = Integer.parseInt(ps.trim()); }
		catch(NumberFormatException ex)
		{
			throw new DataConsumerException("Invalid port '" + ps + "' in host:port argument '" + hp + "'");
		}

		// Use props for before & after strings -- OK if null
		before = PropertiesUtil.getIgnoreCase(props, "before");
		after = PropertiesUtil.getIgnoreCase(props, "after");

		// If not present, will default to false.
		connectPerMessage = TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(props, "connectPerMessage"));

		String s = PropertiesUtil.getIgnoreCase(props, "connectTries");
		if (s != null)
			try { connectTries = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Invalid connectTries property '{}' -- will use default=3", s);
				connectTries = 3;
			}

		s = PropertiesUtil.getIgnoreCase(props, "connectPauseSec");
		if (s != null)
			try { connectPauseSec = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid connectPauseSec property '{}' -- will use default=60", s);
				connectPauseSec = 60;
			}

		// NOTE: Defer opening the connection until we get the first startMessage call.
	}

	@Override
	public void close()
	{
		if (con != null)
		{
			con.disconnect();
			con = null;
		}
	}

	@Override
	public void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		if (con == null)
		{
			for(int n = 0; n < connectTries; n++)
			{
				try
				{
					log.info("Connecting to {}:{}", hostname, port);
					con = new BasicClient(hostname, port);
					con.connect();
					break;
				}
				catch (Exception ex)
				{
					String m = "Cannot connect to " + hostname + ":" + port;

					close();
					if (n == connectTries - 1)
						throw new DataConsumerException(m, ex);
					else
						try { Thread.sleep(connectPauseSec * 1000L); }
						catch(InterruptedException ex2) {}
				}
			}
		}
		if (before != null)
			write(before);
	}

	private void write(String s)
		throws DataConsumerException
	{
		if (con == null)
			return;
		try { con.getOutputStream().write((s + lineSep).getBytes()); }
		catch(Exception ex)
		{
			close();
			throw new DataConsumerException(" Cannot write string '" + s + "'", ex);
		}
	}

	@Override
	public void println(String line)
	{
		try { write(line); }
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot write '{}'", line);
			close();
		}
	}

	@Override
	public void endMessage()
	{
		try
		{
			if (after != null)
				write(after);
			if (con != null)
				con.getOutputStream().flush();
		}
		catch(Exception ex) {}
		if (connectPerMessage)
			close();
	}

	public OutputStream getOutputStream()
	{
		return con == null ? null : con.getOutputStream();
	}

	@Override
	public String getArgLabel()
	{
		return "Host:Port";
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}
}
