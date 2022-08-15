package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;
import decodes.sql.DbKey;
import ilex.util.PropertiesUtil;

public class ApiPlatformConfig
{
	private long configId = DbKey.NullKey.getValue();
	
	private String name = null;
	
	private int numPlatforms = 0;
	
	private String description = null;
	
	private ArrayList<ApiConfigSensor> configSensors = 
		new ArrayList<ApiConfigSensor>();
	
	private ArrayList<ApiConfigScript> scripts = 
		new ArrayList<ApiConfigScript>();

	public long getConfigId()
	{
		return configId;
	}

	public void setConfigId(long configId)
	{
		this.configId = configId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getNumPlatforms()
	{
		return numPlatforms;
	}

	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public ArrayList<ApiConfigSensor> getConfigSensors()
	{
		return configSensors;
	}
	
	public void setConfigSensors(ArrayList<ApiConfigSensor> configSensors)
	{
		this.configSensors = configSensors;
	}

	public ArrayList<ApiConfigScript> getScripts()
	{
		return scripts;
	}

	public void setScripts(ArrayList<ApiConfigScript> scripts)
	{
		this.scripts = scripts;
	}

	/** Create a new object from a decodes.db.PlatformConfig */
	public static ApiPlatformConfig fromDecodes(PlatformConfig cfg)
	{
System.out.println("DecodesPlatformConfig.fromDecodes(cfgid=" + cfg.getId() + ", nm=" + cfg.configName);
		ApiPlatformConfig ret = new ApiPlatformConfig();
		ret.setConfigId(cfg.getKey().getValue());
		ret.setName(cfg.configName);
		ret.setDescription(cfg.description);
		ret.setNumPlatforms(cfg.numPlatformsUsing);
		for(ConfigSensor cs : cfg.getSensorVec())
		{
			ApiConfigSensor dcs = new ApiConfigSensor();
			dcs.setSensorNumber(cs.sensorNumber);
			dcs.setSensorName(cs.sensorName);
			dcs.setRecordingMode(cs.recordingMode);
			dcs.setRecordingInterval(cs.recordingInterval);
			dcs.setTimeOfFirstSample(cs.timeOfFirstSample);
			dcs.setAbsoluteMax(cs.absoluteMax);
			dcs.setAbsoluteMin(cs.absoluteMin);
			PropertiesUtil.copyProps(dcs.getProperties(), cs.getProperties());
			for(DataType dt : cs.getDataTypeVec())
				dcs.getDataTypes().put(dt.getStandard(), dt.getCode());
			dcs.setUsgsStatCode(cs.getUsgsStatCode());
System.out.println("add configSensr " + dcs.getSensorNumber());
			ret.configSensors.add(dcs);
		}
		
		for(DecodesScript ds : cfg.decodesScripts)
		{
			ApiConfigScript apiscript = new ApiConfigScript();
			apiscript.setName(ds.scriptName);
			apiscript.setDataOrder(ds.getDataOrder());
			apiscript.setHeaderType(ds.getHeaderType());
			for(ScriptSensor ss : ds.scriptSensors)
			{
				ApiConfigScriptSensor apiss = new ApiConfigScriptSensor();
				apiss.setSensorNumber(ss.sensorNumber);
				ApiUnitConverter apiuc = new ApiUnitConverter();
				apiuc.setFromAbbr("raw");
				apiuc.setToAbbr(ss.rawConverter.toAbbr);
				apiuc.setAlgorithm(ss.rawConverter.algorithm);
				apiuc.setA(ss.rawConverter.coefficients[0]);
				apiuc.setB(ss.rawConverter.coefficients[1]);
				apiuc.setC(ss.rawConverter.coefficients[2]);
				apiuc.setD(ss.rawConverter.coefficients[3]);
				apiuc.setE(ss.rawConverter.coefficients[4]);
				apiuc.setF(ss.rawConverter.coefficients[5]);
				apiss.setUnitConverter(apiuc);
				apiscript.getScriptSensors().add(apiss);
			}

			for(FormatStatement fs : ds.formatStatements)
			{
				ApiScriptFormatStatement dsfs = new ApiScriptFormatStatement();
				dsfs.setSequenceNum(fs.sequenceNum);
				dsfs.setLabel(fs.label);
				dsfs.setFormat(fs.format);
				apiscript.getFormatStatements().add(dsfs);
			}
			ret.getScripts().add(apiscript);
		}

System.out.println("ApiPlatformConfig.fromDecodes " + ret.getScripts().size() + " scripts:");
for(ApiConfigScript acs : ret.getScripts())
{
	System.out.println("\tScript " + acs.getName() + " " + acs.getScriptSensors().size() + " sensors:");
	for (ApiConfigScriptSensor acss : acs.getScriptSensors())
		System.out.println(acss.prettyPrint());
}
		return ret;
	}
	
	/** Create a new object from a decodes.db.PlatformConfig */
	public static PlatformConfig toDecodes(ApiPlatformConfig apiCfg)
	{
		PlatformConfig ret = new PlatformConfig();
		ret.forceSetId(DbKey.createDbKey(apiCfg.getConfigId()));
		ret.configName = apiCfg.getName();
		ret.description = apiCfg.getDescription();
		ret.numPlatformsUsing = apiCfg.getNumPlatforms();
		
		for(ApiConfigSensor acs : apiCfg.getConfigSensors())
		{
			ConfigSensor cs = new ConfigSensor(ret, acs.getSensorNumber());
			cs.sensorName = acs.getSensorName();
			cs.recordingMode = acs.getRecordingMode();
			cs.recordingInterval = acs.getRecordingInterval();
			cs.timeOfFirstSample = acs.getTimeOfFirstSample();
			if (acs.getAbsoluteMax() != null)
				cs.absoluteMax = acs.getAbsoluteMax();
			if (acs.getAbsoluteMin() != null)
				cs.absoluteMin = acs.getAbsoluteMin();
			PropertiesUtil.copyProps(cs.getProperties(), acs.getProperties());
			for(String std : acs.getDataTypes().keySet())
				cs.getDataTypeVec().add(DataType.getDataType(std, acs.getDataTypes().get(std)));
			cs.setUsgsStatCode(acs.getUsgsStatCode());
			ret.addSensor(cs);		
		}

		for(ApiConfigScript apiScript : apiCfg.getScripts())
		{
			DecodesScript ds = new DecodesScript(ret, apiScript.getName());
			ds.setDataOrder(apiScript.getDataOrder());
			ds.scriptType = Constants.scriptTypeDecodes;
			if (apiScript.getHeaderType() != null)
				ds.scriptType = ds.scriptType + ":" + apiScript.getHeaderType();
			for(ApiConfigScriptSensor apiScriptSensor : apiScript.getScriptSensors())
			{
				ScriptSensor ss = new ScriptSensor(ds, apiScriptSensor.getSensorNumber());
				if (apiScriptSensor.getUnitConverter() != null)
				{
					ApiUnitConverter apiUC = apiScriptSensor.getUnitConverter();
					ss.rawConverter = new UnitConverterDb("raw", apiUC.getToAbbr());
					ss.rawConverter.algorithm = apiUC.getAlgorithm();
					if (apiUC.getA() != null) ss.rawConverter.coefficients[0] = apiUC.getA();
					if (apiUC.getB() != null) ss.rawConverter.coefficients[1] = apiUC.getB();
					if (apiUC.getC() != null) ss.rawConverter.coefficients[2] = apiUC.getC();
					if (apiUC.getD() != null) ss.rawConverter.coefficients[3] = apiUC.getD();
					if (apiUC.getE() != null) ss.rawConverter.coefficients[4] = apiUC.getE();
					if (apiUC.getF() != null) ss.rawConverter.coefficients[5] = apiUC.getF();
				}
				ds.addScriptSensor(ss);
			}
			
			for(ApiScriptFormatStatement apiScriptFmt : apiScript.getFormatStatements())
			{
				FormatStatement fs = new FormatStatement(ds, apiScriptFmt.getSequenceNum());
				fs.label = apiScriptFmt.getLabel();
				fs.format = apiScriptFmt.getFormat();
				ds.formatStatements.add(fs);
			}
			
			ret.addScript(ds);
		}

		return ret;
	}

	
}
