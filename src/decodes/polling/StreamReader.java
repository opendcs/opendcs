/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
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

import ilex.util.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Used by Protocol to continually read the InputStream coming from
 * the remote station. It provides methods for searching the stream for a
 * match-string. It keeps track of characters already searched. It saves
 * the entire stream for subsequent logging. It can 'capture' a portion of
 * the stream for inclusion in a DCP Message.
 */
public class StreamReader
	extends Thread
{
	private String module = "StreamReader";
	private InputStream in = null;
	private ByteArrayOutputStream captured = new ByteArrayOutputStream();
	private boolean capture = false;
	private PollSessionLogger psLog = null;
	private static final int MAX_BUF_SIZE = 99900;
	private byte[] sessionBuf = new byte[MAX_BUF_SIZE];
	private int sessionIdx = 0;
	private int processIdx = 0;
	private boolean _shutdown = false;
	private StreamReaderOwner owner = null;
	
	public StreamReader(InputStream in, StreamReaderOwner owner)
	{
		this.in = in;
		this.owner = owner;
		module = module + "(" + owner.getModule() + ")";
	}
	
	@Override
	public void run()
	{
		Logger.instance().debug1(module + " starting.");
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
						Logger.instance().debug1(module + " input stream closed.");
						_shutdown = true;
					}
					else
					{
						if (sessionIdx >= sessionBuf.length)
							throw new IOException("Message too long. Size=" + sessionIdx);
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
			Logger.instance().debug1(module + " done. lastChar=" + (char)c + " 0x" + Integer.toHexString(c));
		}
		catch (IOException ex)
		{
			Logger.instance().debug1(module + " " + ex);
			owner.inputError(ex);
			_shutdown = true;
		}
		Logger.instance().debug1(module + " exiting.");
	}
	
	public void shutdown()
	{
		Logger.instance().debug1(module + " shutdown() called.");
		_shutdown = true;
		// Note: mustn't close the InputStream, Multiple StreamReaders
		// may be used at various phases of a session on the same ioPort.
		// try { in.close(); } catch(IOException ex) {}
	}
	
//	private synchronized void growBuffer()
//	{
//		byte newbuf[] = new byte[sessionBuf.length + 8192];
//		for(int i=0; i<sessionIdx; i++)
//			newbuf[i] = sessionBuf[i];
//		sessionBuf = newbuf;
//	}
	
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
			Logger.instance().debug1(module + " Waiting " + sec + " seconds.");
		else
		{
			StringBuilder sb = new StringBuilder(module + " Waiting " + sec + " seconds  for ");
			for(PatternMatcher pm : patternMatcher)
				sb.append("'" + new String(pm.getPattern()) + "' ");
			Logger.instance().debug1(sb.toString());
		}
		for(PatternMatcher pm : patternMatcher)
			pm.setProcessIdx(processIdx);
		
		long endMsec = System.currentTimeMillis() + (long)(sec * 1000);
		while(System.currentTimeMillis() < endMsec)
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
			if (_shutdown)
				throw new IOException("Input stream was shut down.");
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
		Logger.instance().debug1(module + " capture=" + capture + ", processIdx=" + processIdx);
	}
	
	/**
	 * Discard any captured data and set processIdx such that only data received after
	 * this point will be processed by the wait and check methods.
	 */
	public void flushBacklog()
	{
		Logger.instance().debug1(module + " flushing backlog at sessionIdx=" + sessionIdx);
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
	
//	/**
//	 * @return a byte array containing all data received during entire session.
//	 */
//	public byte[] getEntireSession()
//	{
//		byte [] ret = new byte[sessionIdx];
//		for(int i = 0; i<ret.length; i++)
//			ret[i] = sessionBuf[i];
//		Logger.instance().debug1(module + " Returning session buf of length " + ret.length);
//		return ret;
//	}

	public void setPollSessionLogger(PollSessionLogger psLog)
	{
		this.psLog = psLog;
	}
	
	public String getModule() { return module; }

}
