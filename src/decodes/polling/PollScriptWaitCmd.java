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

import java.io.IOException;

public class PollScriptWaitCmd extends PollScriptCommand
{
	private double sec = 0.0;
	private PatternMatcher patternMatcher[] = new PatternMatcher[0];
	private boolean mustMatch = false;
	private String cmdline = "";
	private boolean exclude = false;
	private boolean matchFound = false;

	public PollScriptWaitCmd(PollScriptProtocol owner, double sec, boolean mustMatch, String cmdline)
	{
		super(owner);
		this.sec = sec;
		this.mustMatch = mustMatch;
		this.cmdline = cmdline;
	}
	
	public void addMatch(byte[] match)
	{
		if (patternMatcher.length > 0)
		{
			PatternMatcher newArray[] = new PatternMatcher[patternMatcher.length + 1];
			for(int idx = 0; idx < patternMatcher.length; idx++)
				newArray[idx] = patternMatcher[idx];
			patternMatcher = newArray;
		}
		else
			patternMatcher = new PatternMatcher[1];
		patternMatcher[patternMatcher.length-1] = new PatternMatcher(match);
	}

	@Override
	public void execute() 
		throws ProtocolException
	{
		try
		{
			matchFound = owner.getStreamReader().wait(sec, patternMatcher);
			if (!matchFound && mustMatch)
				throw new ProtocolException("Failed to receive expected response for '" 
					+ cmdline + "' in file " + owner.getScriptFileName());
			else if (matchFound && exclude)
				throw new ProtocolException("Received excluded response for '" 
					+ cmdline + "' in file " + owner.getScriptFileName());
		}
		catch (IOException ex)
		{
			throw new ProtocolException(ex.getMessage());
		}
	}

	public void setMustMatch(boolean mustMatch)
	{
		this.mustMatch = mustMatch;
	}

	public void setExclude(boolean exclude)
	{
		this.exclude = exclude;
	}

	public boolean isMatchFound()
	{
		return matchFound;
	}

}
