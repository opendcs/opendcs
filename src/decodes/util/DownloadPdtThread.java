/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.ServerLock;

/**
An instance of this class downloads the PDT from an URL into a local file,
and then exits.
*/
class DownloadPdtThread extends Thread
{
	private String urlstr;
	private String localfn;
	private static final SimpleDateFormat dayFormat =
		new SimpleDateFormat("yyMMdd");
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	private Pdt existingPdt;

	/** 
	 * Constructor.
	 * @param purlstr URL to download from
	 * @param localfn name of local file to download to.
	 */
	DownloadPdtThread(String urlstr, String localfn, Pdt existingPdt)
	{
		this.urlstr = urlstr;
		this.localfn = localfn;
		this.existingPdt = existingPdt;
	}

	/**
	 * Do the download and then exit.
	 * A YYMMDD date extension is added to the specified local file name
	 * for the download. After the download a copy is made to the specified
	 * name without an extension.
	 * <p>
	 * Finally, any PDT files more than 5 days old are deleted.
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
			Logger.instance().warning("Cannot download PDT because lock file '" + lockpath
				+ "' is either taken by another process or is unwritable.");
			return;
		}
		Logger.instance().info("Obtained lock file '" + lockpath + "' -- proceeding with download.");
		
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		long now = System.currentTimeMillis();
		File datedFile = new File(EnvExpander.expand(localfn)
			+ "." + dayFormat.format(new Date(now)));
		Logger.instance().info("Downloading '" + urlstr
			+ "' to '" + datedFile.getPath() + "'");
		try
		{
			URL url = new URL(urlstr);
			bis = new BufferedInputStream(url.openStream());
			bos = new BufferedOutputStream(new FileOutputStream(datedFile));
			// Note copyStream will close when done.
			FileUtil.copyStream(bis, bos); 
			bis = null;
			bos = null;

			Logger.instance().info("PDT downloaded to '" + datedFile.getPath()
				+ "', file size=" + datedFile.length());
			File existingFile = new File(EnvExpander.expand(localfn));
			Logger.instance().info("Merging PDT to '"+existingFile.getPath()+"'");
			merge2ExistingPdt(datedFile, existingFile);
		}
		catch(MalformedURLException ex)
		{
			Logger.instance().warning("Cannot read PDT from '" + urlstr
				+ "': " + ex);
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot download PDT from '" + urlstr
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
				Logger.instance().info("Deleting old PDT '" 
					+ oldFile.getName() + "'");
				oldFile.delete();
			}
		}
		mylock.releaseLock();
		
		if (existingPdt.getPdtLoadListener() != null)
		{
			existingPdt.getPdtLoadListener().pdtLoaded();
			existingPdt.setPdtLoadListener(null);
		}
	}
	
	/**
	 * Merge the newly-downloaded file into the one currently being used by
	 * applications.
	 * @param newDownloadFile Newly downloaded PDT file
	 * @param existingFile PDT currently being used by applications
	 */
	public void merge2ExistingPdt(File newDownloadFile, File existingFile)
	{
		Pdt newDownloadPdt = new Pdt();
		
		newDownloadPdt.load(newDownloadFile);
		existingPdt.load(existingFile);
		
		int numMods = 0;
		for(PdtEntry newPdtEntry : newDownloadPdt.getEntries())
		{
			PdtEntry existingEntry = existingPdt.find(newPdtEntry.dcpAddress);
			if (existingEntry == null
			 || newPdtEntry.lastmodified.after(existingEntry.lastmodified))
			{
				numMods++;
				existingPdt.put(newPdtEntry);
			}
		}

		if (numMods > 0)
		{
			existingPdt.save(existingFile);
			Logger.instance().info("DownloadPdtThread: saved new pdt to '"
				+ existingFile.getPath() + " with " + numMods 
				+ " modifications");
		}
		else
			Logger.instance().info("DownloadPdtThread: No PDT changes."
				+ " Final PDT has " + Pdt.instance().size() + " entries.");
	}
	
}
