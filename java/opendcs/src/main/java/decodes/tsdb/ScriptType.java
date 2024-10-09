package decodes.tsdb;

public enum ScriptType
{
	PY_Init('I'),
	PY_BeforeTimeSlices('B'),
	PY_TimeSlice('T'),
	PY_AfterTimeSlices('A'),
	ToolTip('P'),
	Undefined('U');

	/** The character identifying this script type in the database */
	char dbChar;
	
	private ScriptType(char dbChar)
	{
		this.dbChar = dbChar;
	}
	
	public static ScriptType fromDbChar(char dbChar)
	{
		for(ScriptType sc : values())
			if (sc.dbChar == dbChar)
				return sc;
		return Undefined;
	}
	
	public char getDbChar()
	{
		return dbChar;
	}
}
