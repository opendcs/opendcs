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
package lrgs.archive;

import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;


/**
This class contains static methods for reading and writing a day file's
minute-index. The file is a binary array of 1440 MsgIndexMinute objects.
*/
public class MinuteFile
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	 * Loads the array of MsgIndexMinute entries from the named file.
	 * @param fn the file name.
	 * @param mns the array of MsgIndexMinute structures to fill.
	 */
	public static void load( String fn, MsgIndexMinute[] mns )
		throws IOException
	{
		
		try (DataInputStream dis = new DataInputStream(
					new BufferedInputStream(
						new FileInputStream(fn))))
		{

			for(int i=0; i<mns.length; i++)
			{
				mns[i].startIndexNum = dis.readInt();
				mns[i].reserved = dis.readInt();
				mns[i].oldestDapsTime = dis.readInt();
			}
		}
		catch(IOException ex)
		{
			long t = System.currentTimeMillis();
			if ((t % (24L * 3600000L)) > 60000L) // after 1st minute of day?
			{
				log.atWarn().setCause(ex).log("{} - Error reading '{}'", MsgArchive.EVT_BAD_MINUTE_FILE, fn);
			}
			throw ex;
		}
	}
	
	/**
	 * Saves the array of MsgIndexMinute entries to the named file.
	 * @param fn the file name.
	 * @param mns the array of MsgIndexMinute structures to save.
	 */
	public static void save( String fn, MsgIndexMinute[] mns )
		throws IOException
	{
		try (DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(
						new FileOutputStream(fn))))
		{
			for(int i=0; i<mns.length; i++)
			{
				dos.writeInt(mns[i].startIndexNum);
				dos.writeInt(mns[i].reserved);
				dos.writeInt(mns[i].oldestDapsTime);
			}
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("{} - Error writing '", MsgArchive.EVT_BAD_MINUTE_FILE, fn);
			throw ex;
		}
	}
}