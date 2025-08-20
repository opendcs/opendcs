package lrgs.multistat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Vector;
import java.util.Iterator;

import ilex.util.Logger;
import ilex.util.EnvExpander;

public class AlarmMaskList
{
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
			try
			{
				LineNumberReader lnr = 
					new LineNumberReader(new FileReader(maskFile));
				String line;
				while((line = lnr.readLine()) != null)
				{
					line = line.trim();
					if (line.length() == 0 || line.charAt(0) == '#')
						continue;
					String tok[] = line.split(" ");
					if (tok.length < 3)
					{
						Logger.instance().warning("too few tokens");
						continue;
					}
					int alarmNum = 0;
					try { alarmNum = Integer.parseInt(tok[2]); }
					catch(NumberFormatException ex)
					{
						Logger.instance().warning("bad alarm format");
						continue;
					}
					AlarmMask am = new AlarmMask(tok[0], tok[1], alarmNum);
					maskList.add(am);
				}
				lnr.close();
			}
			catch(IOException ex)
			{
				System.out.println("Cannot read '" + maskFile.getPath()
					+ "': " + ex);
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
