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
package lrgs.multistat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import ilex.util.EnvExpander;

public class AlarmMaskList
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	Vector maskList;
	File maskFile;
	long lastLoadTime;

	public AlarmMaskList()
	{
		maskList = new Vector();
		maskFile = new File(EnvExpander.expand(
			"$DCSTOOL_HOME/alarm.ignore"));
		lastLoadTime = 0L;
	}

	public void check()
	{
		if (maskFile.canRead()
		 && maskFile.lastModified() > lastLoadTime)
		{
			lastLoadTime = System.currentTimeMillis();
			load();
		}
	}

	public synchronized void load()
	{
		if (maskFile.canRead())
		{
			maskList.clear();
			try (FileReader fr = new FileReader(maskFile))
			{
				LineNumberReader lnr = new LineNumberReader(fr);
				String line;
				while((line = lnr.readLine()) != null)
				{
					line = line.trim();
					if (line.length() == 0 || line.charAt(0) == '#')
						continue;
					String tok[] = line.split(" ");
					if (tok.length < 3)
					{
						log.warn("too few tokens");
						continue;
					}
					int alarmNum = 0;
					try { alarmNum = Integer.parseInt(tok[2]); }
					catch(NumberFormatException ex)
					{
						log.atWarn().setCause(ex).log("bad alarm format");
						continue;
					}
					AlarmMask am = new AlarmMask(tok[0], tok[1], alarmNum);
					maskList.add(am);
				}
				lnr.close();
			}
			catch(IOException ex)
			{
				log.atError().setCause(ex).log("Cannot read '{}'", maskFile.getPath());
			}
		}
	}

	/**
	 * Return true if the referenced alarm is masked.
	 * @param host the host name
	 * @param module the module name
	 * @param num the alarm number.
	 * @return true if the referenced alarm is masked.
	 */
	public synchronized boolean isMasked(String host, String module, int num)
	{
		for(Iterator it = maskList.iterator(); it.hasNext(); )
		{
			AlarmMask am = (AlarmMask)it.next();
			if (am.matches(host, module, num))
				return true;
		}
		return false;
	}
}

class AlarmMask
{
	String host;    // non-case-sensitive
	String module;  // non-case-sensitive
	int alarmNum;

	public AlarmMask(String h, String m, int n)
	{
		host = h;
		module = m;
		alarmNum = n;
	}
	public String toString()
	{
		return host + " " + module + " " + alarmNum;
	}

	public boolean matches(String h, String m, int n)
	{
		return
		    (host.equals("*") || host.equalsIgnoreCase(h))
		 && module.equalsIgnoreCase(m)
		 && alarmNum == n;
	}
}