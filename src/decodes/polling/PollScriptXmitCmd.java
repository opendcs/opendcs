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

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;

import java.io.IOException;

public class PollScriptXmitCmd extends PollScriptCommand
{
	public static final String module = "PollScriptXmitCmd";
	private String str = null;

	public PollScriptXmitCmd(PollScriptProtocol owner, String str)
	{
		super(owner);
		this.str = str;
	}

	@Override
	public void execute() 
		throws ProtocolException
	{
		byte []data = null;
		try
		{
			// Expand the string with the platform/site's properties.
			String estr = EnvExpander.expand(str, owner.getProperties());
			
			// Convert to binary to handle escape sequences like \r \n and \002.
			data = AsciiUtil.ascii2bin(estr);
			owner.getIoPort().getOut().write(data);
			
			// The session logger gets the evaluated data, but converted back to String object.
			if (owner.getPollSessionLogger() != null)
				owner.getPollSessionLogger().sent(new String(data));
		}
		catch (IOException ex)
		{
			throw new ProtocolException(module + " error writing '" + new String(data) + "': " + ex);
		}
	}

}
