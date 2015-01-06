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
	public static final String module = "StreamReader";
	private InputStream in = null;
	private ByteArrayOutputStream captured = new ByteArrayOutputStream();
	private boolean capture = false;
	private PollSessionLogger psLog = null;
	private byte[] sessionBuf = new byte[8192];
	private int sessionIdx = 0;
	private int processIdx = 0;
	private boolean _shutdown = false;
	private PollScriptProtocol owner = null;
	
	public StreamReader(InputStream in, PollScriptProtocol owner)
	{
		this.in = in;
		this.owner = owner;
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
							growBuffer();
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
	}
	
	public void shutdown()
	{
		Logger.instance().debug1(module + " shutdown() called.");
		_shutdown = true;
		try { in.close(); } catch(IOException ex) {}
	}
	
	private synchronized void growBuffer()
	{
		byte newbuf[] = new byte[sessionBuf.length + 8192];
		for(int i=0; i<sessionIdx; i++)
			newbuf[i] = sessionBuf[i];
		sessionBuf = newbuf;
	}
	
	/**
	 * From the current processing point forward, wait for the match string
	 * to appear in the input, or the specified number of seconds to elapse.
	 * @param sec floating point number of seconds
	 * @param match String to search for, or null if unconditional wait
	 * @return true if match string was found, false if sec expires.
	 */
	public boolean wait(double sec, byte[] matchBytes)
	{
		Logger.instance().debug1(module + " Waiting " + sec + " seconds for " 
			+ (matchBytes==null ? "(null)" : new String(matchBytes)));
		long endMsec = System.currentTimeMillis() + (long)(sec * 1000);
		while(System.currentTimeMillis() < endMsec)
		{
			if (matchBytes != null && check(matchBytes))
				return true;
			try { sleep(50L); } catch(InterruptedException ex) {}
		}
		return false; // sec timed out without match
	}
	
	/**
	 * Checks current data in buffer from last processing point for a match.
	 * Must be synchronized with growBuffer.
	 * @param matchBytes the bytes to look for.
	 * @return true if match found, false if not.
	 */
	public synchronized boolean check(byte[] matchBytes)
	{
		int mbidx = 0;
		while(processIdx + matchBytes.length <= sessionIdx)
		{
			Logger.instance().debug3(module + " check(" + processIdx + "): '" 
				+ (char)(sessionBuf[processIdx + mbidx]) + "' '" + (char)(matchBytes[mbidx]) + "'");
			if (sessionBuf[processIdx + mbidx] == matchBytes[mbidx])
			{
				if (++mbidx >= matchBytes.length)
				{	
					processIdx += mbidx;
					return true;
				}
			}
			else // doesn't match, start over
			{
				mbidx = 0;
				processIdx++;
			}
		}
		return false;
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
	 * @return a byte array containing all data received when capture was ON.
	 */
	public byte[] getCapturedData()
	{
		return captured.toByteArray();
	}
	
	/**
	 * @return a byte array containing all data received during entire session.
	 */
	public synchronized byte[] getEntireSession()
	{
		byte [] ret = new byte[sessionIdx];
		for(int i = 0; i<ret.length; i++)
			ret[i] = sessionBuf[i];
		Logger.instance().debug1(module + " Returning session buf of length " + ret.length);
		return ret;
	}

	public void setPollSessionLogger(PollSessionLogger psLog)
	{
		this.psLog = psLog;
	}

}
