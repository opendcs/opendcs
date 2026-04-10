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
package lrgs.drgsrecv;

import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 *
 * @author mjmaloney
 * @deprecated use decodes.util.Pdt instead
 */
@Deprecated
public class PdtSched
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private Vector pdtSched;
	private int shortLines, badAddresses, badSecondaryChan, badPrimaryChan,
		badStTimes;

	public PdtSched()
	{
		pdtSched = new Vector();
	}

	/**
	 * Loads a PDT file into memory.
	 * If an IO error occurs, the pdtSched is restored to what is was before
	 * the call to this method.
	 * @param file the file to load.
	 * @return true if load was successful, false if not.
	 */
	public synchronized boolean load(File file)
	{
		log.info("Loading PDT from '{}'", file.getPath());
		Vector oldSched = pdtSched;
		pdtSched = new Vector();
		shortLines = badAddresses = badSecondaryChan = badPrimaryChan =
			badStTimes = 0;
		try
		{
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String line;
			while( (line = lnr.readLine() ) != null)
			{
				PdtSchedEntry pse = convert(line);
				if (pse != null)
					pdtSched.add(pse);
			}
			lnr.close();
		}
		catch(IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("IO Error reading PDT File '{}': -- Old PDT restored.", file.getPath());
			pdtSched = oldSched;
			return false;
		}
		Collections.sort(pdtSched);
		log.info("Parsed PDT File '{}' good entries={}, shortLines={}, " +
				 "badAddresses={}, badSecondaryChan={}, badPrimaryChan={}, badStTimes={}",
				 file.getPath(), pdtSched.size(), shortLines, badAddresses,
				 badSecondaryChan, badPrimaryChan, badStTimes);
		return true;
	}

	/**
	 * Retrieve the PdtSchedEntry for the specified DCP address.
	 * @param dcpAddr the DCP Address
	 * @return the PdtSchedEntry or null if there is none.
	*/
	public synchronized PdtSchedEntry find(long dcpAddr)
	{
		PdtSchedEntry testpse = new PdtSchedEntry(dcpAddr, 0, 0, 0, 0, 0);
		int idx = Collections.binarySearch(pdtSched, testpse);
		if (idx >= 0)
			return (PdtSchedEntry)pdtSched.elementAt(idx);
		else
			return null;
	}

	public PdtSchedEntry convert(String line)
	{
		int len = line.length();
		if (len < 38)
		{
			shortLines++;
			return null;
		}
		String addrs = line.substring(6, 6+8);
		long addr = -1;
		try { addr = Long.parseLong(addrs, 16); }
		catch(NumberFormatException ex)
		{
			badAddresses++;
			return null;
		}
		int stChan = -1;
		int rdChan = -1;
		char t = line.charAt(14);
		try
		{
			if (t == 'S')
				stChan = Integer.parseInt(line.substring(15,15+3));
			else if (t == 'R')
				rdChan = Integer.parseInt(line.substring(15,15+3));
		}
		catch(NumberFormatException ex)
		{
			badPrimaryChan++;
			return null;
		}
		t = line.charAt(18);
		try
		{
			if (t == 'S')
				stChan = Integer.parseInt(line.substring(19,19+3));
			else if (t == 'R')
				rdChan = Integer.parseInt(line.substring(19,19+3));
		}
		catch(NumberFormatException ex)
		{
			badSecondaryChan++;
			return null;
		}
		int xmitSOD = 0;
		int interval = 0;
		int window = 0;
		if (stChan != -1)
		{
			try
			{
				String s = line.substring(22, 22+6);
				xmitSOD = parseSeconds(s);
				s = line.substring(28, 28+6);
				interval = parseSeconds(s);
				s = line.substring(34, 34+4);
				window = parseSeconds(s);
			}
			catch(NumberFormatException ex)
			{
				badStTimes++;
				return null;
			}
		}
		return
			new PdtSchedEntry(addr, stChan, rdChan, window, interval, xmitSOD);
	}

	private int parseSeconds(String s)
	{
		int sec = 0;
		int i = 0;
		if (s.length() == 6)
		{
			sec = ((int)s.charAt(0) - 48) * 10*60*60
				+ ((int)s.charAt(1) - 48) *    60*60;
			i = 2;
		}
		sec += ((int)s.charAt(i++) - 48) *     10*60;
		sec += ((int)s.charAt(i++) - 48) *        60;
		sec += ((int)s.charAt(i++) - 48) *        10;
		sec += ((int)s.charAt(i++) - 48)            ;
		return sec;
	}

	/**
	 * This is for testing only -- to iterate and print out the list.
	 */
	public Iterator iterator()
	{
		return pdtSched.iterator();
	}
}