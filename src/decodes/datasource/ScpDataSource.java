package decodes.datasource;

//import org.apache.sshd.client.SshClient;
//import org.apache.sshd.client.future.ConnectFuture;
//import org.apache.sshd.client.scp.ScpClient;
//import org.apache.sshd.client.session.ClientSession;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3FileHandle;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.Variable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.util.PropertySpec;

public class ScpDataSource 
	extends DataSourceExec
{
	private String module = "ScpDataSource";
	private PropertySpec[] scpDsPropSpecs =
	{
		new PropertySpec("host", PropertySpec.HOSTNAME,
			"SCP Data Source: Host name or IP Address of SSH Server"),
		new PropertySpec("port", PropertySpec.INT,
			"SCP Data Source: Listening port on SSH Server (default = 22)"),
		new PropertySpec("username", PropertySpec.STRING,
			"SCP Data Source: User name with which to connect to SSH server"),
		new PropertySpec("password", PropertySpec.STRING,
			"SCP Data Source: Password on the SSH server"),
		new PropertySpec("remoteDir", PropertySpec.STRING,
			"SCP Data Source: remote directory"),
		new PropertySpec("localDir", PropertySpec.DIRECTORY,
			"SCP Data Source: optional local directory in which to store downloaded"
			+ " file."),
		new PropertySpec("filenames", PropertySpec.STRING,
			"Space-separated list of files to download from server"),
		new PropertySpec("nameIsMediumId", PropertySpec.BOOLEAN,
			"Use with OneMessageFile=true if the downloaded filename is to be treated as a medium ID"
			+ " in order to link this data with a platform."),
		new PropertySpec("useSftp", PropertySpec.BOOLEAN, "Use SFTP rather than SCP.")
	};
	
	private String host = null;
	private int port = 22;
	private String username = "anon";
	private String password = null;
	private String remoteDir = "";
	private String filenames = "";
	// String filename = null; Use protected 'filename' from FileDataSource base class.
	private String localDir = null;
	private Properties allProps = null;
	private ArrayList<File> downloadedFiles = new ArrayList<File>();
	private FileDataSource currentFileDS = null;
	private int downloadedFileIndex = 0;
	private String mySince=null, myUntil=null;
	private Vector<NetworkList> myNetworkLists;
	private File currentFile = null;
	private boolean useSftp = false;
	
	public boolean setProperty(String name, String value)
	{
		if (name.equalsIgnoreCase("host"))
			host = value;
		else if (name.equalsIgnoreCase("port"))
		{
			try { port = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Non-numeric port '" + value
					+ "' -- will use default of 21.");
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
		else if (name.equalsIgnoreCase("filenames"))
			filenames = value;
		else if (name.equalsIgnoreCase("useSftp"))
			useSftp = TextUtil.str2boolean(value);
		return true;
	}		
	
	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		// Remove 'filename' from file data source specs, but keep everything else.
		FileDataSource fds = new FileDataSource();
		PropertySpec[] x = fds.getSupportedProps();
		PropertySpec[] y = new PropertySpec[x.length-1];
		int xidx = 0, yidx = 0;
		for(; xidx < x.length; xidx++)
			if (!x[xidx].getName().equalsIgnoreCase("filename"))
				y[yidx++] = x[xidx];
		
		return PropertiesUtil.combineSpecs(y, scpDsPropSpecs);
	}

	@Override
	public void processDataSource() throws InvalidDatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG3, 
			module + ".processDataSource '" + getName() 
			+ "', args='" +dbDataSource.getDataSourceArg()+"'");
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
		SCPClient scpCli = null;
		SFTPv3Client sftpCli = null;
		Connection conn = new Connection(host, port);
		
		String action = " connecting to SSH server " + host + ":" + port;
		try
		{
			Logger.instance().debug1(module + action);
			conn.connect();
			action = " authenticating as user '" + username + "'";
			boolean isAuthenticated = conn.authenticateWithPassword(username, password);
			if (!isAuthenticated)
				throw new DataSourceException(module + " SSH authentication failed (bad password)");
			Logger.instance().debug1(module + " Password authentication successfull.");
			
			if (!useSftp)
			{
				action = " building SCPSclient";
				Logger.instance().debug1(module + action);
				scpCli = new SCPClient(conn);
			}
			else
			{	
				action = " building SFTPv3Client";
				Logger.instance().debug1(module + action);
				sftpCli = new SFTPv3Client(conn);
			}
		}
		catch(Exception ex)
		{
			try { conn.close(); } catch(Exception ex2) {}
			throw new DataSourceException(module + " Error while" + action 
				+ " server=" + host + ":" + port + ", username=" + username + ": " + ex);
		}
		
		String fns[] = filenames.split(" ");
		downloadedFiles.clear();
		Logger.instance().debug3(module + " there are " + fns.length + " filenames in the list:");
	
		for(String filename : fns)
		{
			SFTPv3FileHandle handle = null;
			OutputStream os = null;
			try
			{
				File localFile = new File(localDir, filename);
				action = " opening output file '" + localFile.getPath() + "'";
				Logger.instance().debug1(module + action);
				os = new FileOutputStream(localFile);
	
				if (useSftp)
				{
					action = " SFTP opening read-only file '" + filename + "' on server";
					Logger.instance().debug1(module + action);
					handle = sftpCli.openFileRO(filename);
					byte buf[] = new byte[1024];
					
					long offset = 0L;
					int len = 0;
					int totalLen = 0;
					action = " SFTP reading data from remote file";
					Logger.instance().debug1(module + action);
					while((len = sftpCli.read(handle, offset, buf, 0, buf.length)) != -1)
					{
						os.write(buf, 0, len);
						totalLen += len;
					}
					sftpCli.closeFile(handle);
					Logger.instance().debug1(module + " SFTP Downloaded file '" + filename + "' len=" + totalLen);
					downloadedFiles.add(localFile);
				}
				else // default uses SCP.
				{
					action = " SCP downloading remote file '" + remotefile + "'";
					String remoteFile = remoteDir
						+ (remoteDir.length() == 0 ? "" : "/")
						+ filename;
					Logger.instance().debug1(module + action);
					scpCli.get(remoteFile, os);
					downloadedFiles.add(localFile);
				}
			}
			catch(Exception ex)
			{
				Logger.instance().warning(module + " Error while" + action + ": " + ex);
			}
			finally
			{
				if (os != null)
					try { os.flush(); os.close(); } catch(Exception ex) {}
				if (handle != null)
					try { sftpCli.closeFile(handle); } catch(Exception ex) {}
			}
		}

		if (sftpCli != null)
			sftpCli.close();
		conn.close();
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
		currentFileDS = new FileDataSource();
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
	public RawMessage getRawMessage() 
		throws DataSourceException
	{
		if (currentFileDS == null)
			throw new DataSourceEndException(module + " file delegate aborted.");
		try
		{
			return currentFileDS.getRawMessage();
		}
		catch(DataSourceEndException ex)
		{
			Logger.instance().info(module + " End of file '" 
				+ currentFile.getPath() + "'");
			openNextFile();
			return getRawMessage(); // recursive call with newly opened file.
		}
		catch(DataSourceException ex)
		{
			Logger.instance().warning(module + " Error processing file '" 
				+ currentFile.getPath() + "': " + ex);
			openNextFile();
			return getRawMessage(); // recursive call with newly opened file.
		}
	}
	
	public static void main(String args[])
		throws Exception
	{
		if (args.length != 5)
		{
			System.out.println("Usage: java class host user pw remotefile");
			System.exit(1);
		}
		String host = args[0];
		String user = args[1];
		String pw = args[2];
		String remoteFile = args[3];
		String localFile = args[4];
		
		Connection conn = new Connection(host);
		conn.connect();
		boolean isAuthenticated = conn.authenticateWithPassword(user, pw);
		if (!isAuthenticated)
		{
			System.err.println("Authentication failed.");
			System.exit(1);
		}
		
		SCPClient cli = new SCPClient(conn);
		OutputStream os = new FileOutputStream(localFile);
		cli.get(remoteFile, os);
		os.flush();
		os.close();
		conn.close();
	}

}
