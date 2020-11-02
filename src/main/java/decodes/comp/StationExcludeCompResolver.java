package decodes.comp;

import java.util.ArrayList;
import java.util.Date;

import opendcs.dai.PlatformStatusDAI;

import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.NetworkList;
import decodes.decoder.DecodedMessage;
import decodes.tsdb.DbIoException;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

public class StationExcludeCompResolver 
	extends CompResolver
	implements PropertiesOwner
{
	private static final String module = "StationExcludeCompResolver";
	private PropertySpec propSpecs[] = 
	{
		new PropertySpec("StationExcludeEnable", PropertySpec.BOOLEAN, 
			"Set to true to enable the Station Exclude Filter module"),
		new PropertySpec("StationExcludeNLPrefix", PropertySpec.STRING, 
			"Any network list that starts with this prefix will be used" +
			" as a list of stations which should be excluded from the output." +
			"(default='exclude_')")
	};
	private boolean isEnabled = false;
	private String netlistPrefix = "exclude_";
	private long lastNetlistCheck = 0L;
	private String rsName = "";
	private ArrayList<NetworkList> excludeList = new ArrayList<NetworkList>();

	@Override
	public Computation[] resolve(IDataCollection msg)
	{
		if (!isEnabled)
			return null;
		
		// The work is done here in the resolver. No need for another class.
		// Check to see if the netlists need to be reloaded periodically
		if (System.currentTimeMillis() - lastNetlistCheck > 60000L)
			checkNetlists();
		
		// If the platform for this message is on the exclude list,
		// Issue an INFO message and delete all the data from the message.
		if (!(msg instanceof DecodedMessage))
			return null;
		DecodedMessage dm = (DecodedMessage)msg;
		Platform p = dm.getPlatform();
		if (p == null)
			return null;
		
		for(NetworkList nl : excludeList)
			if (nl.contains(p))
			{
				Logger.instance().info(module 
					+ " Removing all time series from message for station "
					+ p.getDisplayName() + " because it is on the exclude-"
					+ "network list '" + nl.name + "'");
				dm.rmAllTimeSeries();
				PlatformStatusDAI platformStatusDAO = Database.getDb().getDbIo().makePlatformStatusDAO();
				if (platformStatusDAO != null)
				{
					try
					{
						PlatformStatus platStat = 
							platformStatusDAO.readPlatformStatus(p.getId());
						platStat.setAnnotation("Excluded: " 
							+ rsName + " netlist=" + nl.name);
						platformStatusDAO.writePlatformStatus(platStat);
					}
					catch (DbIoException ex)
					{
						Logger.instance().warning(module
							+ " Cannot access platform status: " + ex);
					}
					finally
					{
						platformStatusDAO.close();
					}
				}
				break;
			}
		
		// Always return null.
		return null;
	}
	

	private void checkNetlists()
	{
		excludeList.clear();
		Database db = Database.getDb();
		for(NetworkList nl : db.networkListList.getList())
			if (nl.name.startsWith(netlistPrefix))
				excludeList.add(nl);
		try
		{
			for(NetworkList nl : excludeList)
			{
				Date dbLMT = db.getDbIo().getNetworkListLMT(nl);
				if (dbLMT != null && nl.lastModifyTime.compareTo(dbLMT) < 0)
				{
					// This indicates that this list was modified.
					nl.clear();
					nl.read();
					nl.prepareForExec();
					Logger.instance().info(module +  
						" Reloaded network list '" + nl.name + "'");
				}
			}
		}
		catch(Exception ex)
		{
			Logger.instance().warning(module + 
				" Exception checking network lists: " + ex);
		}
	}

	@Override
	public void init(RoutingSpec routingSpec)
	{
		String s = routingSpec.getProperty("StationExcludeEnable");
		isEnabled = s != null && TextUtil.str2boolean(s);
		if (!isEnabled)
			return;
		
		s = routingSpec.getProperty("StationExcludeNLPrefix");
		if (s != null)
			netlistPrefix = s;
		rsName = routingSpec.getName();
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}
