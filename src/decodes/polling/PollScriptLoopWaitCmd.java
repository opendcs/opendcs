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

/**
 * The LOOPWAIT command marks the beginning of a simple loop.
 * Nested loops are not allowed.
 * The LOOPEND command marks the end of the loop. If the final WAIT
 * in the loop body succeeded, then processing continues. Otherwise, processing
 * is rewound to the LOOPWAIT command, the count is incremented. If the count
 * has expired then an exception is thrown, otherwise the loop is attempted again.
 */
public class PollScriptLoopWaitCmd extends PollScriptCommand
{
	public static final String module = "PollScriptLoopWaitCmd";
	private int iterations;
	private int count = 0;

	public PollScriptLoopWaitCmd(PollScriptProtocol owner, int iterations)
	{
		super(owner);
		this.iterations = iterations;
	}

	@Override
	public void execute() 
		throws ProtocolException
	{
		if (count++ >= iterations)
			throw new ProtocolException(module + " " + iterations 
				+ " attempted without finding match. Script=" + owner.getScriptFileName());
	}
}
