package decodes.util;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.ServerLock;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
An instance of this class downloads the chans_by_baud file from 
https://dcs1.noaa.gov/chans_by_baud.txt
*/
public class DownloadChannelMapThread extends Thread
{
	String purlstr;
	String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	private ChannelMap cmap;

	/** 
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	DownloadChannelMapThread(String purlstr, String localfn, ChannelMap cmap)
	{
		this.purlstr = purlstr;
		this.localfn = localfn;
		this.cmap = cmap;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any Channel files more than 5 days old are deleted.
	 */
	public void run()
	{
		ServerLock mylock = null;
		/** Optional server lock ensures only one instance runs at a time. */
		String lockpath = EnvExpander.expand(localfn + ".lock");
		mylock = new ServerLock(lockpath);
		mylock.setCritical(false);
		if (!mylock.obtainLock())
		{
			Logger.instance().warning("Cannot download channel map because lock file '" + lockpath
				+ "' is either taken by another process or is unwritable.");
			return;
		}
		Logger.instance().info("Obtained lock file '" + lockpath + "' -- proceeding with download.");
		
		//==========================
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		long now = System.currentTimeMillis();
		File localFile = new File(EnvExpander.expand(localfn)
			+ "." + dayFormat.format(new Date(now)));
		Logger.instance().info("Downloading '" + purlstr
			+ "' to '" + localFile.getPath() + "'");
		try
		{
			URL url = new URL(purlstr);
			bis = new BufferedInputStream(url.openStream());
			bos = new BufferedOutputStream(new FileOutputStream(localFile));
			// Note copyStream will close when done.
			FileUtil.copyStream(bis, bos); 
			bis = null;
			bos = null;

			Logger.instance().info("CDT Download complete, file size=" + localFile.length());
			File current = new File(EnvExpander.expand(localfn));
			Logger.instance().info("Copying Channels to " +	"'" + current.getPath()+"'");
			FileUtil.copyFile(localFile, current);
			cmap.load(current);
		}
		catch(MalformedURLException ex)
		{
			Logger.instance().warning("Cannot read Channels from '" + purlstr
				+ "': " + ex);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot download Channels from '" + purlstr + "': " + ex);
		}
		finally
		{
			if (bis != null)
			{
				try{ bis.close(); } catch(IOException ex) {}
			}
			if (bos != null)
			{
				try{ bos.close(); } catch(IOException ex) {}
			}
		}
		for(int i=5; i<60; i++)
		{
			File oldFile = new File(EnvExpander.expand(localfn)
				+ "." + dayFormat.format(new Date(now - i * MS_PER_DAY)));
			if (oldFile.exists())
			{
				Logger.instance().info("Deleting old Channels '" 
					+ oldFile.getName() + "'");
				oldFile.delete();
			}
		}
		mylock.releaseLock();
	}
}
