package integration.cove.azul;

import java.io.File;

import ilex.net.BasicClient;
import ilex.util.FileUtil;

/**
 * Replay old iridium files, updating the date/times to the current date/time.
 * Usage:
 *    java ... covesw.azul.test.IridiumReplay host[:port] directory
 */
public class IridiumReplay
	extends Thread
{
	private String host;
	private int port;
	private File directory;
	
	public static void main(String args[])
		throws Exception
	{
		if (args.length != 2)
		{
			System.err.println("Requres 2 arguments: ");
			System.err.println("java ... covesw.azul.test.IridiumReplay host[:port] directory");
			System.exit(1);
		}
		
		String host = args[0];
		int port = 10800;
		int colon = host.indexOf(':');
		if (colon >= 0)
		{
			host = host.substring(0, colon);
			port = Integer.parseInt(host.substring(colon+1));
		}
		
		File directory = new File(args[1]);
		if (!directory.isDirectory())
		{
			System.err.println("'" + args[1] + "' is not a directory.");
			System.err.println("java ... covesw.azul.test.IridiumReplay host[:port] directory");
			System.exit(1);
		}
		IridiumReplay iridiumReplay = new IridiumReplay(host, port, directory);
		iridiumReplay.start();
	}
	
	public IridiumReplay(String host, int port, File directory)
	{
		this.host = host;
		this.port = port;
		this.directory = directory;
	}
	
	@Override
	public void run()
	{
		File files[] = directory.listFiles();
		for(File file : files)
		{
			processFile(file);
			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}

	private void processFile(File file)
	{
		System.out.println("Processing file '" + file.getPath() + "'");
		try
		{
			byte[] data = FileUtil.getfileBytes(file);
			
			// xmit time is in Unix time_t format (big endian). Replace it with current time.
			int nowtt = (int)(System.currentTimeMillis()/1000L);
			data[30] = (byte)((nowtt>>24) & 0xff); 
			data[31] = (byte)((nowtt>>16) & 0xff);
			data[32] = (byte)((nowtt>>8)  & 0xff);
			data[33] = (byte)( nowtt      & 0xff);
			
			BasicClient client = new BasicClient(host, port);
			client.connect();
			client.sendData(data);
			client.disconnect();
		}
		catch (Exception ex)
		{
			System.err.println("Error: " + ex);
			ex.printStackTrace();
		}
	}

}
