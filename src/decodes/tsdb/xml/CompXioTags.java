/*
* $Id$
*/
package decodes.tsdb.xml;

/**
Constant tags for storing Comp Meta Data in XML Files.
*/
public class CompXioTags
{
	public static final String compMetaData = "CompMetaData";
	public static final String algorithm = "Algorithm";
	public static final String computation = "Computation";
	public static final String loadingApplication = "LoadingApplication";
	public static final String appProperty = "AppProperty";
	public static final String execClass = "ExecClass";
	public static final String comment = "Comment";
	public static final String algoProperty = "AlgoProperty";
	public static final String algoParm = "AlgoParm";
	public static final String parmType = "ParmType";
	public static final String algorithmName = "AlgorithmName";
	public static final String compProcName = "CompProcName";
	public static final String lastModified = "LastModified";
	public static final String enabled = "Enabled";
	public static final String validStart = "ValidStart";
	public static final String validEnd = "ValidEnd";
	public static final String compProperty = "CompProperty";
	public static final String compParm = "CompParm";
	public static final String interval = "Interval";
	public static final String tableSelector = "TableSelector";
	public static final String deltaT = "DeltaT";
	public static final String deltaTUnits = "DeltaTUnits";
	public static final String modelId = "ModelId";
	public static final String siteDataType = "SiteDataType";
	public static final String siteName = "SiteName";
	public static final String nameType = "NameType";
	public static final String groupName = "GroupName";
	public static final String dataType = "DataType";
	public static final String standard = "Standard";
	public static final String code = "code";

	public static final String roleName = "roleName";
	public static final String name = "name";
	public static final String hdb = "hdb";
	
	// For Time Series Group block:
	// <TsGroup name="MROW4-ROWI4 Observed Stage">
	//   <GroupType>basin</GroupType>
	//   <Description>This is a group description</Description>
	//   <OfficeId>MVR</OfficeId>
	//   <TimeSeries>Unique-String-Id-for-Time-Series</TimeSeries>
	//   <SiteName>MROI4</SiteName>
	//   <SiteName>ROWI4</SiteName>
	//   <DataType standard="SHEF-PE" code="HG"/>
	//   <DataType standard="SHEF-PE" code="HR"/>
	//   <Member type="StatisticsCode" value="Observed"/>
	//   <Member type="Interval" value="15min"/>
	//   <SubGroup include="true">groupname</SubGroup> -- for backward compat
	//   <SubGroup combine="add">groupname</SubGroup>
	// </TsGroup> 
	public static final String tsGroup = "TsGroup";
	public static final String groupType = "GroupType";
	public static final String description = "Description";
	public static final String officeId = "OfficeId";
	public static final String member = "Member";
	public static final String type = "type";
	public static final String value = "value";
	public static final String timeSeries = "TimeSeries";
	public static final String subGroup = "SubGroup";
	public static final String include = "Include";
	public static final String combine = "combine";
	public static final String add = "add";
	public static final String subtract = "subtract";
	public static final String intersect = "intersect";
	
}
