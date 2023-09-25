package org.opendcs.odcsapi.opendcs_dep;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
//import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;

import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiDataSourceRef;
import org.opendcs.odcsapi.dao.ApiDataSourceDAO;
import org.opendcs.odcsapi.dao.ApiRoutingDAO;
import org.opendcs.odcsapi.dao.ApiTsDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * This class runs DECODES routing spec in a sub-program for retrieving raw
 * messages and doing test decoding.
 */
public class DecodesRunner
{
	private static final String module = "DecodesRunner";
	/**
	 * Synchronize static to prevent multiple clients using the apiRawMsgRs routing spec at the
	 * same time.
	 * @throws SQLException 
	 */
	public synchronized static ApiRawMessage getRawMessage(String tmtype, String tmid, DbInterface dbi)
		throws DbException, WebAppException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).finest(module + ".getRawMessage");
		String dcstoolHome = System.getProperty("DCSTOOL_HOME");
		if (dcstoolHome == null)
			throw new WebAppException(ErrorCodes.BAD_CONFIG,
				"Cannot run DECODES because DCSTOOL_HOME is not defined in the environment.");
		Logger.getLogger(ApiConstants.loggerName).finest(module + ".getRawMessage DCSTOOL_HOME=" + dcstoolHome);
		String cmd = dcstoolHome + "/bin/rs -S \"now - 12 hours\" -U now "
				+ "-p sc:DCP_ADDRESS_0000=" + tmid + " " + getRawRs(dbi).getName();
		
		Logger.getLogger(ApiConstants.loggerName).info(module + ".getRawMessage "
				+ "running command '" + cmd + "'");
		try
		{
			String cmdRet = runCmd(cmd);

			// The command may return multiple messages in reverse time order. Get only
			// the first one.
			if (cmdRet.length() > 38)
			{
				String lenField = cmdRet.substring(32, 37);
				try
				{
					int dataLen = Integer.parseInt(lenField);
					cmdRet = cmdRet.substring(0, 37 + dataLen);
				}
				catch(NumberFormatException ex)
				{
					Logger.getLogger(ApiConstants.loggerName).warning("Cannot parse length field '" + lenField
						+ "' -- will return entire cmd results.");
				}
			}
			ApiRawMessage ret = new ApiRawMessage();
			ret.setBase64(new String(Base64.getEncoder().encodeToString(cmdRet.getBytes())));
			return ret;
		}
		catch (Exception ex)
		{
			throw new WebAppException(ErrorCodes.IO_ERROR,
				"Cannot execute command '" + cmd + "': " + ex);
		}
	}
	
	/**
	 * Create or Update the raw msg routing spec used by the API.
	 * It is synchronized static to prevent multiple threads calling.
	 * @throws SQLException 
	 */
	private static synchronized ApiRouting getRawRs(DbInterface dbi)
		throws DbException, WebAppException, SQLException
	{
		try(ApiRoutingDAO rsDao = new ApiRoutingDAO(dbi))
		{
			ApiRouting apiRawMsgRs = null;
			Long routingId = rsDao.getRoutingId("apiRawMsgRs");
			if (routingId == null)
			{
				Logger.getLogger(ApiConstants.loggerName).info(
					module + ".getRawRs No 'apiRawMsgRs' routing spec. Will create.");
				apiRawMsgRs = new ApiRouting();
				apiRawMsgRs.setName("apiRawMsgRs");
				apiRawMsgRs.setDestinationType("pipe");
				apiRawMsgRs.setDestinationArg(null);
				apiRawMsgRs.setOutputFormat("raw");
				apiRawMsgRs.setOutputTZ("UTC");
				apiRawMsgRs.setSince("now - 12 hours");
				apiRawMsgRs.setUntil("now");
				apiRawMsgRs.getPlatformIds().clear();
				apiRawMsgRs.getNetlistNames().clear();
				apiRawMsgRs.getPlatformNames().clear();
				apiRawMsgRs.setEnableEquations(false);
				apiRawMsgRs.setPresGroupName(null);
				apiRawMsgRs.setProduction(false);
				apiRawMsgRs.setAscendingTime(false);
				
				ApiDataSourceRef dataSourceRef = getApiDataSource(dbi);
				apiRawMsgRs.setDataSourceId(dataSourceRef.getDataSourceId());
				apiRawMsgRs.setDataSourceName(dataSourceRef.getName());
				
				apiRawMsgRs.getPlatformIds().clear();
				apiRawMsgRs.getPlatformNames().clear();
				apiRawMsgRs.getNetlistNames().clear();
				apiRawMsgRs.getGoesChannels().clear();

				apiRawMsgRs.getProperties().clear();

				rsDao.writeRouting(apiRawMsgRs);
			}
			else
				apiRawMsgRs = rsDao.getRouting(routingId);
				
			return apiRawMsgRs;
		}
	}
	
	/**
	 * 
	 * @param dbi
	 * @return
	 * @throws DbException
	 */
	private static ApiDataSourceRef getApiDataSource(DbInterface dbi)
		throws DbException, WebAppException
	{
		try(ApiDataSourceDAO dsDao = new ApiDataSourceDAO(dbi);
			ApiTsDAO tsDao = new ApiTsDAO(dbi))
		{
			Properties tsdbProps = tsDao.getTsdbProperties();
			String dsName = tsdbProps.getProperty("api.datasource");
			ArrayList<ApiDataSourceRef> dataSourceRefs = dsDao.readDataSourceRefs();
			for(ApiDataSourceRef dsr : dataSourceRefs)
				if ((dsName == null && dsr.getType().toLowerCase().equals("lrgs"))
				 || (dsName != null && dsName.equalsIgnoreCase(dsr.getName())))
				{
					return dsr;
				}
			throw new WebAppException(ErrorCodes.BAD_CONFIG,
				"No usable LRGS datasource: Create one, then define 'api.datasource' in TSDB properties.");
		}
	}
		
	/**
	* Run a command and return its standard output as a string.
	*/
	private static String runCmd(final String cmd)
		throws IOException
	{
		Process proc = Runtime.getRuntime().exec(cmd);

		// Start a separate thread to read the input stream.
		final InputStream is = proc.getInputStream();
		final ByteArrayOutputStream cmdOut = new ByteArrayOutputStream();
		Thread isr = 
			new Thread()
			{
				public void run()
				{
					try
					{
						int n;
						byte buf[] = new byte[1024];
						while((n = is.read(buf)) > 0)
							cmdOut.write(buf, 0, n);
					}
					catch(IOException ex) {}
				}
			};
		isr.start();

		// Likewise for the stderr stream
		final InputStream es = proc.getErrorStream();
		Thread esr =
			new Thread()
			{
				public void run()
				{
					try
					{
						byte buf[] = new byte[1024];
						int n = es.read(buf);
						if (n > 0)
							Logger.getLogger(ApiConstants.loggerName).warning(
								"cmd(" + cmd + ") stderr returned(" + n + ") '"
								+ new String(buf, 0, n) + "'");
					}
					catch(IOException ex) {}
				}
			};
		esr.start();
 
		// Finally, wait for process and catch its exit code.
		try
		{
            int exitStatus = proc.waitFor();
            // Race-condition, after process ends, wait a sec for
            // reads in isr & esr above to finish.
            Thread.sleep(1000L);
			if (exitStatus != 0)
				Logger.getLogger(ApiConstants.loggerName).warning("cmd(" + cmd + ") exit status "
					+ exitStatus);
        }
        catch(InterruptedException ex)
        {
        }
		
		return cmdOut.toString();
	}
}
