package decodes.snotel;

import decodes.comp.CompResolver;
import decodes.comp.Computation;
import decodes.comp.IDataCollection;
import decodes.db.RoutingSpec;
import decodes.routing.RoutingScheduler;
import decodes.tsdb.TsdbAppTemplate;
import ilex.util.Logger;

public class ConfigCompResolver extends CompResolver
{
	private static ConfigMonitor cfgMonitorInstance = null;	
	public static String module = "SnotelFileMonitor";
	private boolean createdInstance = false;
	private String configDir = "$DCSTOOL_USERDIR/snotelConfig";
	private String realtimeDir = "$DCSTOOL_USERDIR/snotelRealtime";
	private String historyDir = "$DCSTOOL_USERDIR/historyRealtime";

	public ConfigCompResolver()
	{
	}

	@Override
	public Computation[] resolve(IDataCollection msg)
	{
		// always return empty list.
		return new Computation[0];
	}

	@Override
	public synchronized void init(RoutingSpec routingSpec)
	{
		TsdbAppTemplate myApp = TsdbAppTemplate.getAppInstance();
		if (myApp == null || !(myApp instanceof RoutingScheduler))
		{
			Logger.instance().info(module + 
				".init - Running stand-alone. config file processing disabled.");
			return;
		}
		else
			Logger.instance().info(module + ".init - Running under RoutingScheduler. Will enable"
					+ " special config file processing.");
		

		if (cfgMonitorInstance == null)
		{
			createdInstance = true;
			cfgMonitorInstance = new ConfigMonitor();
			cfgMonitorInstance.setConfigDir(configDir);
			cfgMonitorInstance.setRealtimeDir(realtimeDir);
			cfgMonitorInstance.setHistoryDir(historyDir);

			cfgMonitorInstance.start();
			
			// Set routing scheduler to refresh its schedule every 5 sec.
			((RoutingScheduler)myApp).refreshSchedInterval = 5L;
		}
	}

	@Override
	public void setProperty(String name, String value)
	{
		// Note, setProperty is called before init()
Logger.instance().info(module + ".setProperty(" + name + ", " + value + ")");

		super.setProperty(name, value);
		if (name.equalsIgnoreCase("configDir"))
			configDir = value;
		else if (name.equalsIgnoreCase("realtimeDir"))
			realtimeDir = value;
		else if (name.equalsIgnoreCase("historyDir"))
			historyDir = value;
		else
			Logger.instance().warning(module + " unrecognized property name '"
				+ name + "' -- ignored.");
	}

	@Override
	public void shutdown()
	{
		super.shutdown();
	}

}
