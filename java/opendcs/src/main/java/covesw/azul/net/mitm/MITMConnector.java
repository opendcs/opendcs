package covesw.azul.net.mitm;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MITMConnector 
	extends Thread
{
	private InputStream from;
	private OutputStream to;
	private String pfx;
	private byte buf[] = new byte[1024];
	boolean shutdown = false;
	MITMConnector mate = null;
	MITMLogger logger = null;
	
	public MITMConnector(String pfx, InputStream from,
		OutputStream to, MITMLogger logger)
	{
		this.from = from;
		this.to = to;
		this.pfx = pfx;
		this.logger = logger;
	}
	
	@Override
	public void run()
	{
		try
		{
			while(!shutdown)
			{
				int len = from.read(buf, 0, buf.length);
				if (len == 0)
					try { sleep(100L); } catch(InterruptedException ex) {}
				else if (len > 0)
				{
					logger.log(pfx, buf, len);
					to.write(buf, 0, len);
					to.flush();
				}
				else if (len < 0)
				{
					shutdown = true;
					mate.shutdown = true;
				}
			}
		}
		catch(Exception ex)
		{
			shutdown = true;
			System.out.println("pfx=" + pfx + " terminated by exception: ex");
			ex.printStackTrace();
		}
		if (mate != null)
			mate.shutdown = true;
	}
}
