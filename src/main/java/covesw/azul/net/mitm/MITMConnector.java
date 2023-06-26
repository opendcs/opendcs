package covesw.azul.net.mitm;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.slf4j.helpers.Util.getCallingClass;


public class MITMConnector 
	extends Thread
{
	private static final Logger logger = LoggerFactory.getLogger(getCallingClass());
	private InputStream from;
	private OutputStream to;
	private String pfx;
	private byte buf[] = new byte[1024];
	boolean shutdown = false;
	MITMConnector mate = null;
	MITMLogger mitmLogger = null;
	
	public MITMConnector(String pfx, InputStream from,
		OutputStream to, MITMLogger mitmLogger)
	{
		this.from = from;
		this.to = to;
		this.pfx = pfx;
		this.mitmLogger = mitmLogger;
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
					mitmLogger.log(pfx, buf, len);
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
			logger.atError()
			      .setCause(ex)
				  .setMessage("pfx={}terminated by exception: ex")
				  .addArgument(pfx)
				  .log();
		}
		if (mate != null)
			mate.shutdown = true;
	}
}
