/*
*  $Id$
*/
package decodes.xml;

/**
 * This class contains the constant string tags used for element and
 * attribute names within the DECODES XML files. All tags are stored
 * in this one class to provide a single coherent map. The logic that
 * converts the various elements into objects is divided amongst several
 * parser classes.
 */
public class XmlDbTags
{
	// Generic tags used at various points in the hierarchy:
	public static final String description_el = "Description";
	public static final String propertyName_at = "PropertyName";
	public static final String sensorNumber_at = "SensorNumber";
	public static final String DataType_el = "DataType";
	public static final String DataType_standard_at = "Standard";
	public static final String DataType_code_at = "Code";
	public static final String dataOrder_el = "DataOrder";
	public static final String label_at = "Label";
	public static final String name_at = "Name";
	public static final String id_at = "ID";
	public static final String name_el = "Name";
	public static final String isProduction_el = "IsProduction";
	public static final String expiration_el = "Expiration";
	public static final String sequenceNum_at = "SequenceNum";
	public static final String type_at = "Type";

	// Object-specific tags:
	public static final String EnumList_el = "EnumList";
	public static final String Enum_el = "Enum";
	public static final String EnumDefaultValue_el = "EnumDefaultValue";
	public static final String EnumValue_el = "EnumValue";
	public static final String EnumValue_value_at = "EnumValue";
	public static final String execClass_el = "ExecClass";
	public static final String editClass_el = "EditClass";
	public static final String sortNumber_el = "SortNumber";

	public static final String Platform_el = "Platform";
	public static final String agency_el = "Agency";
	public static final String lastModifyTime_el = "LastModifyTime";
	public static final String OperationalProfile_el = "OperationalProfile";

	public static final String PlatformList_el = "PlatformList";
	public static final String PlatformXref_el = "PlatformXref";
	public static final String PlatformId_at = "PlatformId";
	public static final String TransportXref_el = "TransportXref";
	public static final String PlatformProperty_el = "PlatformProperty";
	public static final String PlatformDesignator_el = "PlatformDesignator";

	public static final String TransportMedium_el = "TransportMedium";
	public static final String TransportMedium_mediumType_at = "MediumType";
	public static final String TransportMedium_mediumId_at = "MediumId";
	public static final String channelNum_el = "ChannelNum";
	public static final String assignedTime_el = "AssignedTime";
	public static final String transmitWindow_el = "TransmitWindow";
	public static final String transmitInterval_el = "TransmitInterval";
	public static final String timeAdjustment_el = "TimeAdjustment";
	public static final String preamble_el = "Preamble";

	public static final String Site_el = "Site";
	public static final String latitude_el = "Latitude";
	public static final String longitude_el = "Longitude";
	public static final String elevation_el = "Elevation";
	public static final String elevationUnits_el = "ElevationUnits";
	public static final String timezone_el = "Timezone";
	public static final String country_el = "Country";
	public static final String state_el = "State";
	public static final String nearestCity_el = "NearestCity";
	public static final String region_el = "Region";
	public static final String SiteProperty_el = "SiteProperty";

	public static final String SiteName_el = "SiteName";
	public static final String SiteName_nameType_at = "NameType";
	public static final String SiteName_usgsDbno_at = "UsgsDbno";
	public static final String SiteName_agencyCode_at = "AgencyCode";

	public static final String PlatformConfig_el = "PlatformConfig";
	public static final String PlatformConfig_configName_at = "ConfigName";
	public static final String ConfigSensor_el = "ConfigSensor";
	public static final String ConfigSensorProperty_el = "ConfigSensorProperty";
	public static final String sensorName_el = "SensorName";
	public static final String recordingMode_el = "RecordingMode";
	public static final String recordingInterval_el = "RecordingInterval";
	public static final String timeOfFirstSample_el = "TimeOfFirstSample";
	public static final String AbsoluteMin_el = "AbsoluteMin";
	public static final String AbsoluteMax_el = "AbsoluteMax";
	public static final String UsgsStatCode_el = "UsgsStatCode";
	

	public static final String PlatformSensor_el = "PlatformSensor";
	public static final String PlatformSensorProperty_el = "PlatformSensorProperty";
	public static final String UsgsDdno_el = "UsgsDdno";
	
	public static final String EquipmentModel_el = "EquipmentModel";

	public static final String company_el = "Company";
	public static final String model_el = "Model";
	public static final String equipmentType_el = "EquipmentType";
	public static final String EquipmentProperty_el = "EquipmentProperty";

	public static final String DecodesScript_el = "DecodesScript";
	public static final String DecodesScript_scriptName_at = "ScriptName";
	public static final String scriptType_el = "ScriptType";
	public static final String FormatStatement_el = "FormatStatement";
	public static final String ScriptSensor_el = "ScriptSensor";

	public static final String UnitConverter_el = "UnitConverter";
	public static final String UnitConverter_toUnitsAbbr_at = "ToUnitsAbbr";
	public static final String UnitConverter_fromUnitsAbbr_at = "FromUnitsAbbr";
	public static final String algorithm_el = "Algorithm";
	public static final String a_el = "A";
	public static final String b_el = "B";
	public static final String c_el = "C";
	public static final String d_el = "D";
	public static final String e_el = "E";
	public static final String f_el = "F";

	public static final String DataTypeEquivalenceList_el = "DataTypeEquivalenceList";
	public static final String DataTypeEquivalence_el = "DataTypeEquivalence";
	public static final String EquationSpec_el = "EquationSpec";
	public static final String OutputName_el = "OutputName";
	public static final String Scope_el = "Scope";
	public static final String ApplyTo_el = "ApplyTo";
	public static final String UnitsAbbr_el = "UnitsAbbr";
	public static final String EqStatement_el = "EqStatement";
	public static final String VarName_el = "VarName";
	public static final String Expression_el = "Expression";
	public static final String EquationSpecList_el = "EquationSpecList";

	public static final String EqTable_el = "EqTable";
	public static final String lookupAlgorithm_el = "LookupAlgorithm";
	public static final String inputName_el = "InputName";
	public static final String applyInputLowerBound_el = "ApplyInputLowerBound";
	public static final String inputLowerBound_el = "InputLowerBound";
	public static final String applyInputUpperBound_el = "ApplyInputUpperBound";
	public static final String inputUpperBound_el = "InputUpperBound";
	public static final String EqTableProperty_el = "EqTableProperty";
	public static final String EqTablePoint_el = "EqTablePoint";
	public static final String x_at = "X";
	public static final String y_at = "Y";
	
	public static final String EngineeringUnitList_el = "EngineeringUnitList";
	public static final String EngineeringUnit_el = "EngineeringUnit";
	public static final String EngineeringUnit_abbr_at = "UnitsAbbr";
	public static final String Family_el = "Family";
	public static final String Measures_el = "Measures";

	public static final String RoutingSpec_el = "RoutingSpec";
	public static final String DataSource_el = "DataSource";
	public static final String EnableEquations_el = "EnableEquations";
	public static final String UsePerformanceMeasurements_el = "UsePerformanceMeasurements";
	public static final String OutputFormat_el = "OutputFormat";
	public static final String OutputTimeZone_el = "OutputTimeZone";
	public static final String PresentationGroupName_el = "PresentationGroupName";
	public static final String ConsumerType_el = "ConsumerType";
	public static final String ConsumerArg_el = "ConsumerArg";
	public static final String SinceTime_el = "SinceTime";
	public static final String UntilTime_el = "UntilTime";
	public static final String RoutingSpecNetworkList_el = "RoutingSpecNetworkList";
	public static final String RoutingSpecProperty_el = "RoutingSpecProperty";
	public static final String DataSourceArg_el = "DataSourceArg";
	public static final String DataSourceGroupMember_el = "DataSourceGroupMember";

	public static final String NetworkList_el = "NetworkList";
	public static final String TransportMediumType_el = "TransportMediumType";
	public static final String SiteNameTypePreference_el = "SiteNameTypePreference";
	public static final String NetworkListEntry_el = "NetworkListEntry";
	public static final String TransportId_at = "TransportId";
	public static final String PlatformName_el = "PlatformName";

	public static final String PresentationGroup_el = "PresentationGroup";
	public static final String InheritsFrom_el = "InheritsFrom";
	public static final String DataPresentation_el = "DataPresentation";
	public static final String EquipmentModelName_el = "EquipmentModelName";

	public static final String RoundingRule_el = "RoundingRule";
	public static final String SignificantDigits_el = "SignificantDigits";
	public static final String MaxDecimals_el = "MaxDecimals";
	public static final String UpperLimit_el = "UpperLimit";
	public static final String MinValue_el = "MinValue";
	public static final String MaxValue_el = "MaxValue";

	public static final String PMConfigList_el = "PerformanceMeasurementsList";
	public static final String PMConfig_el = "PerformanceMeasurements";
	
	public static final String TimeZone_el = "TimeZone";
	//public static final String TimeZone_abbr_at =    "Abbreviation";
	//public static final String GmtOffset_el =        "GmtOffset";
	//public static final String DaylightTime_el =     "DaylightTime";
	//public static final String TimeZoneList_el =     "TimeZoneList";

	public static final String Database_el = "Database";
	
	public static final String IntervalList_el = "IntervalList";
	public static final String Interval_el = "Interval";
	
	public static final String ScheduleEntry_el = "ScheduleEntry";
	public static final String RoutingSpecName_el = "RoutingSpecName";
	public static final String LoadingAppName_el = "LoadingAppName";
	public static final String StartTime_el = "StartTime";
	public static final String RunInterval_el = "RunInterval";
	public static final String Enabled_el = "Enabled";
	
	public static final String PlatformStatus_el = "PlatformStatus";
	public static final String LastContact_el = "LastContact";
	public static final String LastMessageTime_el = "LastMessageTime";
	public static final String LastFailureCodes_el = "FailureCodes";
	public static final String LastErrorTime_el = "LastErrorTime";
	public static final String LastScheduleEntryStatus_el = "LastScheduleEntryStatus";
	public static final String Annotation_el = "Annotation";
	

}

