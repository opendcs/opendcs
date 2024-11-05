/*
*  $Id$
*/
package ilex.net;

import java.net.*;
import java.io.*;

/**
Connect to a server at specified host & port & send a file. Then hangup.
*/
public class FileSendClient
{
	private static boolean jerky = false;
	
	/**
	 * Send a file's contents to a named host & port.
	 * @param filename the file to send
	 * @param host the host to send to
	 * @param port the port to connect to on the host
	 * @return the number of bytes sent
	 * @throws IOException if any error happens in the file or the socket.
	 */
	public static int sendFile(String filename, String host, int port)
		throws IOException
	{
		FileInputStream fis = null;
		Socket sock = null;
		OutputStream output = null;
		try
		{
			sock = new Socket(host, port);
			output = sock.getOutputStream();
			fis = new FileInputStream(filename);
			int total = 0;
			int jerkyTotal = 0;
			int jerkyStop = (int)(Math.random() * 1024.);
			int c;
			while((c = fis.read()) != -1)
			{
				total++;
				output.write((byte)c);
				if (jerky && jerkyTotal++ == jerkyStop)
				{
					System.out.println("pausing after " + jerkyTotal);
					try { Thread.sleep(5000L); }
					catch(InterruptedException ex) {}
					jerkyTotal = 0;
					jerkyStop = (int)(Math.random() * 1024.);
				}
				
			}
			output.flush();
			return total;
		}
		finally
		{
			if (fis != null)
			{
				try { fis.close(); } catch(Exception ex) {}
			}
			if (output != null)
			{
				try { output.close(); } catch(Exception ex) {}
			}
			if (sock != null)
			{
				try { sock.close(); } catch(Exception ex) {}
			}
		}
	}
	/**
	* Main method.
	* @param args args[0]==host, args[1]==port, args[2]==file
	* @throws IOException
	*/
	public static void main( String[] args ) throws IOException
	{
		String host = args[0];
		int port = Integer.parseInt(args[1]);
		if (args.length >= 4 && args[3].equals("jerky"))
			jerky = true;
//		System.err.println("jerky=" + jerky + ", args[2]='" + args[2] + "'");
		int total = sendFile(args[2], host, port);
		System.err.println("Done, wrote " + total + " bytes.");
	}
}

