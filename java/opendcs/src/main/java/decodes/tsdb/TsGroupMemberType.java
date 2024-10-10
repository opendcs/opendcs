package decodes.tsdb;

public enum TsGroupMemberType
{
	Interval,
	Duration,
	ParamType,
	Version,
	BaseVersion,
	SubVersion,
	Param, // Not stored in _other_ table in DB, full param stored as data type
	SubParam,
	BaseParam,
	Location,
	BaseLocation,
	SubLocation
}
