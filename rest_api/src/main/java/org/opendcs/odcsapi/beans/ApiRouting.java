package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class ApiRouting
{
	private Long routingId = null;
	private String name = null;
	private Long dataSourceId = null;
	private String dataSourceName = null;
	private String destinationType = null;
	private String destinationArg = null;
	private boolean enableEquations = false;
	private String outputFormat = null;
	private String outputTZ = null;
	private String presGroupName = null;
	private Date lastModified = null;
	private boolean isProduction = false;
	private String since = null;
	private String until = null;
	private boolean settlingTimeDelay = false;
	private String applyTimeTo = "Local Receive Time";
	private boolean ascendingTime = false;
	
	private ArrayList<String> platformIds = new ArrayList<String>();
	private ArrayList<String> platformNames = new ArrayList<String>();
	private ArrayList<String> netlistNames = new ArrayList<String>();
	private ArrayList<Integer> goesChannels = new ArrayList<Integer>();
	
	private Properties properties = new Properties();
	
	private boolean goesSelfTimed = false;
	private boolean goesRandom = false;
	private boolean networkDCP = false;
	private boolean iridium = false;
	private boolean qualityNotifications = false;
	private boolean goesSpacecraftCheck = false;
	private String goesSpacecraftSelection = "East";
	private boolean parityCheck = false;
	private String paritySelection = "Good";
	
	
	public Long getRoutingId()
	{
		return routingId;
	}
	public void setRoutingId(Long routingId)
	{
		this.routingId = routingId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(Long dataSourceId)
	{
		this.dataSourceId = dataSourceId;
	}
	public String getDataSourceName()
	{
		return dataSourceName;
	}
	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}
	public String getDestinationType()
	{
		return destinationType;
	}
	public void setDestinationType(String destinationType)
	{
		this.destinationType = destinationType;
	}
	public String getDestinationArg()
	{
		return destinationArg;
	}
	public void setDestinationArg(String destinationArg)
	{
		this.destinationArg = destinationArg;
	}
	public boolean isEnableEquations()
	{
		return enableEquations;
	}
	public void setEnableEquations(boolean enableEquations)
	{
		this.enableEquations = enableEquations;
	}
	public String getOutputFormat()
	{
		return outputFormat;
	}
	public void setOutputFormat(String outputFormat)
	{
		this.outputFormat = outputFormat;
	}
	public String getOutputTZ()
	{
		return outputTZ;
	}
	public void setOutputTZ(String outputTZ)
	{
		this.outputTZ = outputTZ;
	}
	public String getPresGroupName()
	{
		return presGroupName;
	}
	public void setPresGroupName(String presGroupName)
	{
		this.presGroupName = presGroupName;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	public boolean isProduction()
	{
		return isProduction;
	}
	public void setProduction(boolean isProduction)
	{
		this.isProduction = isProduction;
	}
	public String getSince()
	{
		return since;
	}
	public void setSince(String since)
	{
		this.since = since;
	}
	public String getUntil()
	{
		return until;
	}
	public void setUntil(String until)
	{
		this.until = until;
	}
	public boolean isSettlingTimeDelay()
	{
		return settlingTimeDelay;
	}
	public void setSettlingTimeDelay(boolean settlingTimeDelay)
	{
		this.settlingTimeDelay = settlingTimeDelay;
	}
	public String getApplyTimeTo()
	{
		return applyTimeTo;
	}
	public void setApplyTimeTo(String applyTimeTo)
	{
		this.applyTimeTo = applyTimeTo;
	}
	public ArrayList<String> getPlatformIds()
	{
		return platformIds;
	}
	public void setPlatformIds(ArrayList<String> platformIds)
	{
		this.platformIds = platformIds;
	}
	public ArrayList<String> getPlatformNames()
	{
		return platformNames;
	}
	public void setPlatformNames(ArrayList<String> platformNames)
	{
		this.platformNames = platformNames;
	}
	public ArrayList<String> getNetlistNames()
	{
		return netlistNames;
	}
	public void setNetlistNames(ArrayList<String> netlistNames)
	{
		this.netlistNames = netlistNames;
	}
	public ArrayList<Integer> getGoesChannels()
	{
		return goesChannels;
	}
	public void setGoesChannels(ArrayList<Integer> goesChannels)
	{
		this.goesChannels = goesChannels;
	}
	public Properties getProperties()
	{
		return properties;
	}
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
	public boolean isGoesSelfTimed()
	{
		return goesSelfTimed;
	}
	public void setGoesSelfTimed(boolean goesSelfTimed)
	{
		this.goesSelfTimed = goesSelfTimed;
	}
	public boolean isGoesRandom()
	{
		return goesRandom;
	}
	public void setGoesRandom(boolean goesRandom)
	{
		this.goesRandom = goesRandom;
	}
	public boolean isNetworkDCP()
	{
		return networkDCP;
	}
	public void setNetworkDCP(boolean networkDCP)
	{
		this.networkDCP = networkDCP;
	}
	public boolean isIridium()
	{
		return iridium;
	}
	public void setIridium(boolean iridium)
	{
		this.iridium = iridium;
	}
	public boolean isQualityNotifications()
	{
		return qualityNotifications;
	}
	public void setQualityNotifications(boolean qualityNotifications)
	{
		this.qualityNotifications = qualityNotifications;
	}
	public boolean isGoesSpacecraftCheck()
	{
		return goesSpacecraftCheck;
	}
	public void setGoesSpacecraftCheck(boolean goesSpacecraftCheck)
	{
		this.goesSpacecraftCheck = goesSpacecraftCheck;
	}
	public String getGoesSpacecraftSelection()
	{
		return goesSpacecraftSelection;
	}
	public void setGoesSpacecraftSelection(String goesSpacecraftSelection)
	{
		this.goesSpacecraftSelection = goesSpacecraftSelection;
	}
	public boolean isParityCheck()
	{
		return parityCheck;
	}
	public void setParityCheck(boolean parityCheck)
	{
		this.parityCheck = parityCheck;
	}
	public String getParitySelection()
	{
		return paritySelection;
	}
	public void setParitySelection(String paritySelection)
	{
		this.paritySelection = paritySelection;
	}
	public boolean isAscendingTime()
	{
		return ascendingTime;
	}
	public void setAscendingTime(boolean ascendingTime)
	{
		this.ascendingTime = ascendingTime;
	}
	

}
