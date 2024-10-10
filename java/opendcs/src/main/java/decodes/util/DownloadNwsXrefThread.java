package decodes.util;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.ServerLock;
import ilex.util.Logger;
import ilex.util.FileServerLock;

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
An instance of this class downloads the NWS Cross Reference file from 
http://www.nws.noaa.gov/oh/hads/USGS/ALL_USGS-HADS_SITES.txt
*/
public class DownloadNwsXrefThread extends Thread
{
	String purlstr;
	String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	private NwsXref cmap;

	/** 
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	DownloadNwsXrefThread(String purlstr, String localfn, NwsXref cmap)
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
		mylock = new FileServerLock(lockpath);
		mylock.setCritical(false);
		if (!mylock.obtainLock())
		{
			Logger.instance().warning("Cannot download NWS Xref because lock file '" + lockpath
				+ "' is either taken by another process or is unwritable.");
			return;
		}
		Logger.instance().info("Obtained lock file '" + lockpath + "' -- proceeding with download.");
		
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

			File current = new File(EnvExpander.expand(localfn));
			if (localFile.length() < current.length()*.9)
			{
				Logger.instance().warning("Download '" + localFile.getPath()
					+ "' resulted in a file that is less than 90% the previous "
					+ "download. Assuming incomplete download.");
			}
			else
			{
				Logger.instance().info("NwsXref Download complete, file size=" 
					+ localFile.length());
				Logger.instance().info("Copying NwsXref to " +
						"'"+current.getPath()+"'");
				FileUtil.copyFile(localFile, current);
				cmap.load(current);
			}
		}
		catch(MalformedURLException ex)
		{
			Logger.instance().warning("Cannot read NwsXref from '" + purlstr
				+ "': " + ex);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot download NwsXref from '" + 
					purlstr
				+ "': " + ex);
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
				Logger.instance().info("Deleting old NwsXref '" 
					+ oldFile.getName() + "'");
				oldFile.delete();
			}
		}
		mylock.releaseLock();
	}
}
