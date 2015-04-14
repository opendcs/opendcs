package covesw.azul.license;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;

/**
 * Miscellaneous System functions
 */
public class SystemInfo
{
	private static String osname = null;
	private static String sn = null;

	public static String getOsName()
	{
		if (osname == null)
			osname = System.getProperty("os.name");
		return osname;
		
	}
	
	public static boolean isMac()
	{
		return getOsName().toLowerCase().contains("mac");
	}
	
	public static boolean isLinux()
	{
		return getOsName().toLowerCase().contains("linux");
	}
	
	public static boolean isWindows()
	{
		return getOsName().toLowerCase().startsWith("win");
	}
	
	public static boolean isSolaris()
	{
		return getOsName().toLowerCase().contains("sunos");
	}
	
	/**
	 * @return Unique system serial number as a string
	 * @throws IOException if serial number cannot be determined or if OS is unrecognized.
	 */
	public static String getSerialNumber()
		throws IOException
	{
		if (sn != null)
			return sn;
		if (isWindows())
			return getSerialWindows();
		else if (isLinux() || isSolaris())
			return getSerialLinux();
		else if (isMac())
			return getSerialMac();
		throw new IOException("Cannot determine OS");
	}

	private static String getSerialWindows()
		throws IOException
	{
		OutputStream os = null;
		InputStream is = null;
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		process = runtime.exec(new String[] { "wmic", "bios", "get", "serialnumber" });
		os = process.getOutputStream();
		is = process.getInputStream();
		os.close();
		Scanner sc = new Scanner(is);
		try
		{
			while (sc.hasNext())
			{
				String next = sc.next();
				if ("SerialNumber".equals(next))
				{
					sn = sc.next().trim();
					break;
				}
			}
		}
		finally
		{
			try
			{
				is.close();
			}
			catch (Exception ex) {}
		}
		if (sn == null)
			throw new IOException("getSerialWindows(): Cannot find computer SN");
		return sn;
	}

	private static final String getSerialLinux()
		throws IOException
	{
		if (sn == null)
			readDmidecode();
		if (sn == null)
			readLshal();
		if (sn == null)
			throw new IOException("getSerialLinux(): Cannot find computer SN");
		return sn;
	}

	private static BufferedReader read(String command)
		throws IOException
	{
		OutputStream os = null;
		InputStream is = null;
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		process = runtime.exec(command.split(" "));
		os = process.getOutputStream();
		is = process.getInputStream();
		try
		{
			os.close();
		}
		catch (Exception ex) {}
		return new BufferedReader(new InputStreamReader(is));
	}

	private static void readDmidecode()
		throws IOException
	{
		String line = null;
		String marker = "Serial Number:";
		BufferedReader br = null;
		try
		{
			br = read("dmidecode -t system");
			while ((line = br.readLine()) != null)
				if (line.indexOf(marker) != -1)
				{
					sn = line.split(marker)[1].trim();
					break;
				}
		}
		finally
		{
			if (br != null)
				try { br.close(); } catch (IOException e) {}
		}
	}

	private static void readLshal()
		throws IOException
	{
		String line = null;
		String marker = "system.hardware.serial =";
		BufferedReader br = null;
		try
		{
			br = read("lshal");
			while ((line = br.readLine()) != null)
			{
				if (line.indexOf(marker) != -1)
				{
					sn = line.split(marker)[1].replaceAll("\\(string\\)|(\\')", "").trim();
					break;
				}
			}
		}
		finally
		{
			if (br != null)
				try { br.close(); } catch (IOException e) {}
		}
	}

	private static final String getSerialMac()
		throws IOException
	{
		if (sn != null)
			return sn;
		OutputStream os = null;
		InputStream is = null;
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		process = runtime.exec(new String[] { "/usr/sbin/system_profiler", "SPHardwareDataType" });
		os = process.getOutputStream();
		is = process.getInputStream();
		try { os.close(); } catch(Exception ex) {}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		String marker = "Serial Number";
		try
		{
			while ((line = br.readLine()) != null)
			{
				if (line.indexOf(marker) != -1)
				{
					int idx = line.indexOf(":");
					if (idx != -1)
						sn = line.substring(idx+1).trim();
					break;
				}
			}
		}
		finally
		{
			try { is.close(); } catch (IOException e) {}
		}
		if (sn == null)
			throw new IOException("getSerialMac(): Cannot find computer SN");
		return sn;
	}

	public static void main(String args[])
		throws Exception
	{
		System.out.println("OS Name:" + getOsName());
		System.out.println("Serial Number: " + getSerialNumber());
	}
}