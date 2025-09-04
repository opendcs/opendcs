/*
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import ilex.util.ArrayUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
 * Used by Protocol to continually read the InputStream coming from
 * the remote station. It provides methods for searching the stream for a
 * match-string. It keeps track of characters already searched. It saves
 * the entire stream for subsequent logging. It can 'capture' a portion of
 * the stream for inclusion in a DCP Message.
 */
public class StreamReader extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String module = "StreamReader";
	private InputStream in = null;
	private ByteArrayOutputStream captured = new ByteArrayOutputStream();
	private boolean capture = false;
	private PollSessionLogger psLog = null;
	public static final int MAX_BUF_SIZE = 99900;
	private byte[] sessionBuf = new byte[MAX_BUF_SIZE];
	private int sessionIdx = 0;
	private int processIdx = 0;
	private boolean _shutdown = false;
	private StreamReaderOwner owner = null;

	public StreamReader(InputStream in, StreamReaderOwner owner)
	{
		this.in = in;
		this.owner = owner;
		if (owner != null)
			module = module + "(" + owner.getModule() + ")";
	}

	@Override
	public void run()
	{
		log.debug("starting.");
		try
		{
			int c=-1;
			while(!_shutdown)
			{
				if (in.available() > 0)
				{
					c = in.read();
					if (c == -1)
					{
						log.debug("input stream closed.");
						_shutdown = true;
					}
					else
					{
						if (sessionIdx >= sessionBuf.length)
						{
							log.info("Message too long. Size={}, truncating message at this point.", sessionIdx);
							_shutdown = true;
							break;
						}
						sessionBuf[sessionIdx++] = (byte)c;
						if (capture)
							captured.write(c);
						if (psLog != null)
							psLog.received((char)c);
					}
				}
				else
				{
					try { sleep(100L); } catch(InterruptedException ex) {}
				}
			}
		}
		catch (IOException ex)
		{
			log.atDebug().setCause(ex).log("Error reading stream.");
			if (owner != null)
				owner.inputError(ex);
			_shutdown = true;
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Unexpected exception.");
			if (owner != null)
				owner.inputError(ex);
			_shutdown = true;

		}
		log.debug("exiting.");
	}

	public void shutdown()
	{
		log.trace("shutdown() called.");
		_shutdown = true;
		// Note: mustn't close the InputStream, Multiple StreamReaders
		// may be used at various phases of a session on the same ioPort.
	}

	/**
	 * From the current processing point forward, wait for the match string
	 * to appear in the input, or the specified number of seconds to elapse.
	 * @param sec floating point number of seconds
	 * @param patternMatcher array of patterns to search for (may be zero-length array)
	 * @return true if match string was found, false if sec expires.
	 * @throws IOException if the input stream was shut down.
	 */
	public boolean wait(double sec, PatternMatcher patternMatcher[])
		throws IOException
	{
		if (patternMatcher.length == 0)
		{
			log.debug("Waiting {} seconds.", sec);
		}
		else
		{
			StringBuilder sb = new StringBuilder(module + " Waiting " + sec + " seconds for ");
			for(PatternMatcher pm : patternMatcher)
				sb.append("'" + new String(pm.getPattern()) + "' ");
			log.debug(sb.toString());
		}
		for(PatternMatcher pm : patternMatcher)
			pm.setProcessIdx(processIdx);

		long endMsec = System.currentTimeMillis() + (long)(sec * 1000);
		while(!_shutdown && System.currentTimeMillis() < endMsec)
		{
			synchronized(this)
			{
				for(PatternMatcher pm : patternMatcher)
					if (pm.check(sessionBuf, sessionIdx))
					{
						processIdx = pm.getProcessIdx();
						return true;
					}
			}
			try { sleep(50L); } catch(InterruptedException ex) {}
		}
		if (patternMatcher.length > 0)
			processIdx = patternMatcher[0].getProcessIdx();
		return false; // sec timed out without match
	}

	/**
	 * Start or stop capturing data for subsequent inclusion in DCP message
	 * @param capture true to turn capture on, false to turn it off.
	 */
	public void setCapture(boolean capture)
	{
		this.capture = capture;
		this.processIdx = sessionIdx;
		log.trace("capture={}, porcessIdx={}", capture, processIdx);
	}

	/**
	 * Discard any captured data and set processIdx such that only data received after
	 * this point will be processed by the wait and check methods.
	 */
	public void flushBacklog()
	{
		log.trace("flushing backlog at sessionIdx={}", sessionIdx);
		this.processIdx = sessionIdx;
		captured.reset();
	}

	/**
	 * @return a byte array containing all data received when capture was ON.
	 */
	public byte[] getCapturedData()
	{
		return captured.toByteArray();
	}

	public void setPollSessionLogger(PollSessionLogger psLog)
	{
		this.psLog = psLog;
	}

	public String getModule() { return module; }

	public byte[] getSessionBuf()
	{
		return ArrayUtil.getField(sessionBuf, 0, sessionIdx);
	}

	public int getSessionIdx() { return sessionIdx; }

	public boolean isCapture()
	{
		return capture;
	}

}
