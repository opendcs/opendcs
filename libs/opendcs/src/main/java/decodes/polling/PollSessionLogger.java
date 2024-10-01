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

import java.io.PrintStream;
import java.util.Date;

public class PollSessionLogger
{
	private char lastOp = '\0';
	private PrintStream out;
	
	public PollSessionLogger(PrintStream out, String sitename)
	{
		this.out = out;
		out.println("Session with station " + sitename + " starting at " + new Date());
		out.println();
		out.flush();
	}
	
	public synchronized void sent(String sent)
	{
		if (lastOp != 'S')
		{
			out.println();
			out.println("SENT:");
		}
		out.print(sent);
		out.flush();
		lastOp = 'S';
	}
	
	public synchronized void received(char c)
	{
		if (lastOp != 'R')
		{
			out.println();
			out.println("RECV:");
		}
		out.write(c);
		out.flush();
		lastOp = 'R';
	}
	
	public synchronized void annotate(String msg)
	{
		out.println();
		out.println("[" + msg + "]");
		out.flush();
	}
	
	public void close()
	{
		if (out != System.out)
			try { out.close(); } catch(Exception ex) {}
	}

}
