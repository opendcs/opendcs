package decodes.util.hads;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;

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
An instance of this class downloads the USGS Hads from an URL into a 
local file and then exits.
*/
public class LoadHadsThread extends Thread
{
	String purlstr;
	String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;

	/** 
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	LoadHadsThread(String purlstr, String localfn)
	{
		this.purlstr = purlstr;
		this.localfn = localfn;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any USGS Hads files more than 5 days old are deleted.
	 */
	public void run()
	{
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		long now = System.currentTimeMillis();
		File localFile = new File(EnvExpander.expand(localfn)
			+ dayFormat.format(new Date(now)));
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

			Logger.instance().info("Download complete, file size=" 
				+ localFile.length());
			File current = new File(EnvExpander.expand(localfn));
			Logger.instance().info("Copying Hads to '"+current.getPath()+"'");
			FileUtil.copyFile(localFile, current);
		}
		catch(MalformedURLException ex)
		{
			Logger.instance().warning("Cannot read HADS from '" + purlstr
				+ "': " + ex);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot download HADS from '" + purlstr
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
				+ dayFormat.format(new Date(now - i * MS_PER_DAY)));
			if (oldFile.exists())
			{
				Logger.instance().info("Deleting old HADS '" 
					+ oldFile.getName() + "'");
				oldFile.delete();
			}
		}
	}
}
