package opendcs.opentsdb;

public class OpenTsdbFlags
{
	public static final int RESERVED_4_COMP               = 0x0000000F;
	public static final int RESERVED_4_IFLAG              = 0xF0000000;
	
	public static final int SCREENED                      = 0x00000010;
	
	public static final int RANGE_RESULT_MASK             = 0x000000E0;
	public static final int RANGE_GOOD                    = 0x00000000;
	public static final int RANGE_REJECT_HIGH             = 0x00000020;
	public static final int RANGE_CRITICAL_HIGH           = 0x00000040;
	public static final int RANGE_WARNING_HIGH            = 0x00000060;
	public static final int RANGE_WARNING_LOW             = 0x00000080;
	public static final int RANGE_CRITICAL_LOW            = 0x000000A0;
	public static final int RANGE_REJECT_LOW              = 0x000000C0;
	
	public static final int ROC_RESULT_MASK               = 0x00000700;
	public static final int ROC_GOOD                      = 0x00000000;
	public static final int ROC_REJECT_HIGH               = 0x00000100;
	public static final int ROC_CRITICAL_HIGH             = 0x00000200;
	public static final int ROC_WARNING_HIGH              = 0x00000300;
	public static final int ROC_WARNING_LOW               = 0x00000400;
	public static final int ROC_CRITICAL_LOW              = 0x00000500;
	public static final int ROC_REJECT_LOW                = 0x00000600;
	
	// The following two are NOT stored in data values, but are used by the
	// alarm system only
	public static final int STUCK_SENSOR_DETECTED         = 0x00000800;
	public static final int MISSING_VALUES_EXCEEDED       = 0x00001000;


	public OpenTsdbFlags()
	{
		// TODO Auto-generated constructor stub
	}

}
