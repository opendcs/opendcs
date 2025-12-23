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
package decodes.datasource;

import ilex.util.EnvExpander;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.util.PropertySpec;

import com.jcraft.jsch.*;

public class SftpDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String module = "SftpDataSource";
	private PropertySpec[] sftpDsPropSpecs =
	{
		new PropertySpec("host", PropertySpec.HOSTNAME,
			"FTP Data Source: Host name or IP Address of FTP Server"),
		new PropertySpec("port", PropertySpec.INT,
			"FTP Data Source: Listening port on FTP Server (default = 22)"),
		new PropertySpec("username", PropertySpec.STRING,
			"FTP Data Source: User name with which to connect to FTP server"),
		new PropertySpec("password", PropertySpec.STRING,
			"FTP Data Source: Password on the FTP server"),
		new PropertySpec("remoteDir", PropertySpec.STRING,
			"FTP Data Source: remote directory - blank means root"),
		new PropertySpec("localDir", PropertySpec.DIRECTORY,
			"FTP Data Source: optional local directory in which to store downloaded"
			+ " file. If not supplied, received file is processed by DECODES but not"
			+ " saved locally."),
		new PropertySpec("filenames", PropertySpec.STRING,
			"Required Space-separated list of files to download from server"),
		new PropertySpec("deleteFromServer", PropertySpec.BOOLEAN,
			"FTP Data Source: (default=false) Set to true to delete file from server "
			+ "after retrieval. (May be disallowed on some servers.)")
	};

	private String host = null;
	private int port = 22;
	private String username = "anon";
	private String password = null;
	private String remoteDir = "";
	private String filenames = "";
	private String localDir = null;
	private boolean deleteFromServer = false;
	private Properties allProps = null;
	private ArrayList<File> downloadedFiles = new ArrayList<File>();
	private FileDataSource currentFileDS = null;
	private int downloadedFileIndex = 0;
	private String mySince=null, myUntil=null;
	private Vector<NetworkList> myNetworkLists;
	private File currentFile = null;

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public SftpDataSource(DataSource source, Database db) {
		super(source, db);
	}

	public boolean setProperty(String name, String value)
	{
		if (name.equalsIgnoreCase("host"))
			host = value;
		else if (name.equalsIgnoreCase("port"))
		{
			try { port = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				log.warn("Non-numeric port '{}' -- will use default of 22.", value);
				port = 22;
			}
		}
		else if (name.equalsIgnoreCase("username"))
			username = value;
		else if (name.equalsIgnoreCase("password"))
			password = value;
		else if (name.equalsIgnoreCase("remoteDir"))
			remoteDir = value;
		else if (name.equalsIgnoreCase("localDir"))
			localDir = value;
		else if (name.equalsIgnoreCase("deleteFromServer"))
			deleteFromServer = TextUtil.str2boolean(value);
		else if (name.equalsIgnoreCase("filenames"))
			filenames = value;
		return true;
	}

	class MyProgMon implements SftpProgressMonitor
	{
		long count = 0;

		@Override
		public boolean count(long count)
		{
			this.count = count;
			return true;
		}

		@Override
		public void end()
		{
		}

		@Override
		public void init(int op, String src, String dest, long max)
		{
			// TODO Auto-generated method stub
		}

		public long getCount()
		{
			return count;
		}
	};


	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		// Remove 'filename' from file data source specs, but keep everything else.
		FileDataSource fds = new FileDataSource(null,null);
		PropertySpec[] x = fds.getSupportedProps();
		PropertySpec[] y = new PropertySpec[x.length-1];
		int xidx = 0, yidx = 0;
		for(; xidx < x.length; xidx++)
			if (!x[xidx].getName().equalsIgnoreCase("filename"))
				y[yidx++] = x[xidx];

		return PropertiesUtil.combineSpecs(y, sftpDsPropSpecs);
	}

	@Override
	public void processDataSource() throws InvalidDatabaseException
	{
		log.trace("processDataSource '{}', args='{}'", getName(), dbDataSource.getDataSourceArg());
	}

	@Override
	public void init(Properties routingSpecProps, String since, String until, Vector<NetworkList> networkLists)
		throws DataSourceException
	{
		mySince = since;
		myUntil = until;
		myNetworkLists = networkLists;

		// Build a complete property set. Routing Spec props override DS props.
		allProps = new Properties(dbDataSource.arguments);
		for(Enumeration<?> it = routingSpecProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = routingSpecProps.getProperty(name);
			allProps.setProperty(name, value);
		}
		for(Enumeration<?> it = allProps.propertyNames(); it.hasMoreElements();)
		{
			String name = (String)it.nextElement();
			String value = allProps.getProperty(name);
			name = name.trim().toLowerCase();
			setProperty(name, value);
		}

		downloadFiles();
		if (downloadedFiles.size() == 0)
			throw new DataSourceException(module + " Failed to download any files.");

		openNextFile();
	}


	private void downloadFiles()
		throws DataSourceException
	{
		// First make sure all the required properties are set.
		if (host == null || host.trim().length() == 0)
			throw new DataSourceException("Missing required 'host' property.");
		if (username == null || username.trim().length() == 0)
			throw new DataSourceException("Missing required 'username' property.");
		if (password == null || password.trim().length() == 0)
			throw new DataSourceException("Missing required 'password' property.");

		// Next download the file using FTP client.
		String action = "Constructing JSch";
		JSch jsch = null;
		Session session = null;
		ChannelSftp chanSftp = null;
		try
		{
			final String realUserName = EnvExpander.expand(username);
			final String realPassword = EnvExpander.expand(password);
			log.trace(action);
			JSch.setConfig("StrictHostKeyChecking", "no");
			jsch = new JSch();

			action = "Getting Session";
			log.trace(action);
			session = jsch.getSession(realUserName, host, port);

			action = "Setting Session Password";
			log.trace(action);
			session.setPassword(realPassword);

			action = "Connecting session";
			log.trace(action);
			session.connect();

			action = "Configuring session";
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			action = "Getting SFTP channel";
			log.trace(action);
			chanSftp = (ChannelSftp)session.openChannel("sftp");

			action = "Connecting SFTP channel";
			log.trace(action);
			chanSftp.connect();

			String remote = remoteDir;
			if (remote.length() > 0 && !remote.endsWith("/"))
				remote += "/";

			action = "Constructing progress monitor";
			MyProgMon myProgMon = new MyProgMon();


			downloadedFiles.clear();
			downloadedFileIndex = 0;

			// split by whitespace* comma
			String fns[] = filenames.split(" ");
			if (log.isTraceEnabled())
			{
				log.trace("There are {} filenames in the list:", fns.length);
				for(String fn : fns)
				{
					log.trace("   '{}'", fn);
				}
			}

			String local = localDir;
			if (local == null || local.length() == 0)
				local = "$DCSTOOL_USERDIR/tmp";
			local = EnvExpander.expand(local);
			File localDirectory = new File(local);
			if (!localDirectory.isDirectory())
				localDirectory.mkdirs();


			for(String filename : fns)
			{
				filename = filename.trim(); // remove any whitespace before or after the comma.
				String remoteName = remote + filename;
				File localFile = new File(localDirectory, filename);

				try
				{
					myProgMon.count(0L);
					action = "Downloading remote file '" + remoteName
							+ "' to '" + localFile.getPath() + "'";
					log.trace(action);
					chanSftp.get(remoteName, localFile.getPath(), myProgMon, ChannelSftp.OVERWRITE);
					log.trace("{} SUCCESS, size={}", action, myProgMon.getCount());
					downloadedFiles.add(localFile);

					action = "Deleting '" + remoteName + "' from server";
					if (deleteFromServer)
						chanSftp.rm(remoteName);
				}
				catch(SftpException ex)
				{
					log.atWarn().setCause(ex).log("Error while {}", action);
				}
			}
		}
		catch(JSchException ex)
		{
			String msg = module + " Error while" + action;
			throw new DataSourceException(msg, ex);
		}
		finally
		{
			if (chanSftp != null && chanSftp.isConnected())
				chanSftp.disconnect();
			if (session != null && session.isConnected())
				session.disconnect();
		}

        log.info("{} files downloaded.", downloadedFiles.size());
	}

	/**
	 * Opens the next file downloaded from FTP by constructing a FileDataSource delegate.
	 * @throws DataSourceEndException exception if there are no more files
	 * @throws DataSourceException exception if thrown in the init method of FileDataSource delegate.
	 */
	private void openNextFile()
		throws DataSourceException
	{
		currentFileDS = null;

		if (downloadedFileIndex >= downloadedFiles.size())
			throw new DataSourceEndException(module + " All " + downloadedFileIndex
				+ " files processed.");

		currentFile = downloadedFiles.get(downloadedFileIndex++);
		currentFileDS = new FileDataSource(this.dbDataSource,db);
		allProps.setProperty("filename", currentFile.getPath());
		if (TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(allProps, "NameIsMediumId")))
			allProps.setProperty("mediumid", currentFile.getName());

		currentFileDS.init(allProps, mySince, myUntil, myNetworkLists);
	}


	@Override
	public void close()
	{
		downloadedFiles.clear();
		if (currentFileDS != null)
			currentFileDS.close();
		currentFileDS = null;
	}

	@Override
	protected RawMessage getSourceRawMessage() 
		throws DataSourceException
	{
		if (currentFileDS == null)
			throw new DataSourceEndException(module + " file delegate aborted.");
		try
		{
			return currentFileDS.getSourceRawMessage();
		}
		catch(DataSourceEndException ex)
		{
			log.info("End of file '{}'", currentFile.getPath());
			openNextFile();
			return getSourceRawMessage(); // recursive call with newly opened file.
		}
		catch(DataSourceException ex)
		{
			log.atWarn().setCause(ex).log(" Error processing file '{}'", currentFile.getPath());
			openNextFile();
			return getSourceRawMessage(); // recursive call with newly opened file.
		}
	}

}
