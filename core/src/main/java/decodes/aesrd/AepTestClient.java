package decodes.aesrd;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class AepTestClient
{
	
	static Station[] stations = 
	{
		new Station("ALBE", "7802661981.eairlink.com", 6785 ),
		new Station("ATH2", "74.198.191.114", 6785 ),
		new Station("AURO", "96.1.79.140", 6785 ),
		new Station("BALL", "7802316519.eairlink.com", 6785 ),
		new Station("BARR", "184.151.142.29", 6785 ),
		new Station("BUSB", "7802661779.eairlink.com", 6785 ),
		new Station("CALG", "74.198.246.96", 6785 ),
		new Station("CAMR", "74.198.224.136", 6785 ),
		new Station("CRET", "7802662489.eairlink.com", 6785 ),
		new Station("DUPR", "5873356968.eairlink.com", 6785 ),
		new Station("HAWK", "7802661686.eairlink.com", 6785 ),
		new Station("KINS", "7802389742.eairlink.com", 6785 ),
		new Station("MUNI", "74.198.230.159", 6785 ),
		new Station("MVIL", "7802661461.eairlink.com", 6785 ),
		new Station("NORD", "184.151.139.176", 6785 ),
		new Station("RADW", "7802662453.eairlink.com", 6785 ),
		new Station("RAIN", "7802647159.eairlink.com", 6785 ),
		new Station("REDI", "5873363150.eairlink.com", 6785 ),
		new Station("SHEN", "7808937160.eairlink.com", 6785 ),
		new Station("STAL", "7802350197.eairlink.com", 6785 ),
		new Station("STEE", "96.1.78.134", 6785 )
	};
	
	static PrintStream results;
	
	/**
	* Main method.
	* @param args args[0]==host, args[1]==port
	* @throws IOException
	*/
	public static void main( String[] args )
		throws Exception
	{
		results = new PrintStream("results.txt");
		
		for (Station station : stations)
		{
			System.out.print("Trying " + station.name + " " + station.host 
				+ ":" + station.port + " ...");
			results.print("Trying " + station.name + " " + station.host 
				+ ":" + station.port + " ...");
			
			Socket sock = null;
			InputStream input = null;
			try
			{
				sock = new Socket(station.host, station.port);
				input = sock.getInputStream();
				System.out.println("Success!");
				results.println("Success!");
			}
			catch (Exception ex)
			{
				System.out.println("Failed: " + ex);
				results.println("Failed: " + ex);
			}
			finally
			{
				if (input != null)
					try { input.close(); } catch(Exception ex) {}
				if (sock != null)
					try { sock.close(); } catch(Exception ex) {}
			}

		}
		results.close();
	}
	
}

class Station
{
	String name;
	String host;
	int port;
	protected Station(String name, String host, int port)
	{
		super();
		this.name = name;
		this.host = host;
		this.port = port;
	}
}

