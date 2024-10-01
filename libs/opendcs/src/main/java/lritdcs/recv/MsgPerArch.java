/**
 * @(#) MsgPerArch.java
 */

package lritdcs.recv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lrgs.common.DcpMsg;


public class MsgPerArch
{
	private int startTime;
	
	private int duration;
	private MsgFile msgFile;

	public static final String filePrefix = "dcp-";
	public static final String dateSpec = "yyyyMMddHH";
	public static SimpleDateFormat fnf = new SimpleDateFormat(dateSpec);
	public static final String fileSuffix = ".msg";
	
	/**
	 * Create a new period archive object, may be for an existing or new file.
	 * @throws FileNotFoundException if file doesn't exist and can't be created.
	 */
	public MsgPerArch(int startTime, int duration)
		throws FileNotFoundException
	{
		this.startTime = startTime;
		this.duration = duration;
		
		String fileName = getFileName(startTime);
		msgFile = new MsgFile(new File(fileName), true);
	}

	public static String getFileName(int startTime)
	{
		return LritDcsRecvConfig.instance().msgFileDir +
			File.separator + filePrefix + fnf.format(new Date(startTime*1000L))
			+ fileSuffix;
	}

	public void add(DcpMsg msg)
		throws IOException
	{
		msgFile.archive(msg);
	}
	
	public void finish( )
	{
		msgFile.close();
		msgFile = null;
	}

	public int getStartTime()
	{
		return startTime;
	}
}
