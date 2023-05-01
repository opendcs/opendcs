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

/**
 * Class used for matching patterns in the POLL Script WAIT command.
 */
public class PatternMatcher
{
	private byte []pattern;
	private int processIdx=0;
	
	/**
	 * constructor
	 * @param pattern The pattern to search for
	 * @param processIdx The location in the session buffer from which to start
	 */
	public PatternMatcher(byte[] pattern)
	{
		this.pattern = pattern;
	}
	
	/**
	 * Check buffer to see if there is a match now
	 * @param sessionBuf the buffer
	 * @param sessionBufLen the total length of the buffer
	 * @return
	 */
	public boolean check(byte[] sessionBuf, int sessionBufLen)
	{
		int mbidx = 0;
		while(processIdx + pattern.length <= sessionBufLen)
		{
//Logger.instance().debug2("PatMat: looking for '" + new String(pattern) + "', sessIdx=" + sessionBufLen + ", procIdx=" + processIdx
//+ ", mbidx=" + mbidx + ", patlen=" + pattern.length + ", c='" + (char)sessionBuf[processIdx + mbidx] + "'");
			if (sessionBuf[processIdx + mbidx] == pattern[mbidx])
			{
				if (++mbidx >= pattern.length)
				{	
//Logger.instance().debug2("PatMat: Match!");
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
//Logger.instance().debug2("PatMat: No match yet.");
		return false;
	}
	
	/** After a match is successfull, StreamReader will call this to find out where to continue processing. */
	public int getProcessIdx() { return processIdx; }

	/** Called prior to first call to check */
	public void setProcessIdx(int processIdx)
	{
		this.processIdx = processIdx;
	}

	public byte[] getPattern()
	{
		return pattern;
	}

}
