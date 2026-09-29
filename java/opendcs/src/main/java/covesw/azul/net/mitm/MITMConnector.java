/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package covesw.azul.net.mitm;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class MITMConnector extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

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
			log.atDebug().setCause(ex).log("{} terminated by exception", pfx);
		}
		if (mate != null)
			mate.shutdown = true;
	}
}
