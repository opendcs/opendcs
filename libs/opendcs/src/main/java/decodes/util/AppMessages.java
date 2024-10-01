/**
 * 
 */
package decodes.util;

/**
 * Create on Jan 4, 2011
 * 
 * @author gchen
 * 
 *
 */

/**
 * This AppDWEMessages is used for the application displaying, warning, and error messages.
 *
 * The message syntax is defined as <msg-name>(<msg-code>, <msg-module>, <msg-description>).
 * 
 * The message output format is defined as the following for 
 *   warning and error messages: "<msg-module>-<msg-code>: <msg-description>"
 *   displaying messages       : "<msg-description>" 
 *
 * The <msg-name> represents the message name with using the following convention
 *   <msg-module>_XXXXXXXXXXXXXXXXX
 * for example, sys_FailReadCfgFile, db_DBConnFail, or decd_DuplicatedData
 * 
 * The <msg-code> is consists of 6 digits
 *   msg-code -->    XXXXXX
 *   position -->    123456
 * where position 1 and 2 for <msg-module>, and position 3-6 for <msg-description>. 999999 is reserved for UNKNOWN message.
 * 
 * The <msg-module> is briefly categorized as system level (SYS, DB, DEBG, UTIL) and application level (DECD, COMP, LRGS).
 * The <msg-module> is defined as the following
 *   00-09: reserved;
 *      10: reserved; 11: SYS;  12: DB;   13: UTIL; 19: DEBG; etc.
 *      20: reserved; 21: DECD; 22: DCFG; 
 *      30: reserved; 31: COMP;
 *      40: reserved; 41: LRGS; 
 */
public enum AppMessages
{
  /**
   * System level messages
   */
	//System messages
	sys_Unknown                       (999999, "SYS", 		"Unknown"),
	sys_ExceptionMessage              (999998, "SYS", 		"%s"),
	sys_FailReadCfgFile               (111001, "SYS", 		"Fail to read the configuration file."),
	
	//Utility messages
	util_CreateFullTSGroupList        (131001, "UTIL", 		"Create full TS group list."),
	util_CreateLimitTSGroupList       (131002, "UTIL", 		"Create limited TS group list."),
	util_SortTSGroupList              (131003, "UTIL", 		"Sort the TS group list."),
	util_LoadTSGroupList              (131004, "UTIL", 		"Load the TS group list."),
	util_EmptyTSGroupList             (131005, "UTIL", 		"TS group list is empty."),
	util_MakeCompXioObj               (131006, "UTIL", 		"Make CompXio object."),
	util_WriteMetadataToXML           (131007, "UTIL", 		"Write the TS groups into the %s XML file."),
	util_SuccessWriteMetadataToXML    (131008, "UTIL", 		"Successful to write the TS groups into the %s XML file."),
	util_FailWriteMetadataToXML       (131009, "UTIL", 		"Failed to write the TS groups into the %s XML file."),
	util_ReadMetadataFromXML          (131010, "UTIL", 		"Read the TS groups from the %s XML file."),
	util_SuccessReadMetadataFromXML   (131011, "UTIL", 		"Successful to read the TS groups from the %s XML file."),
	util_FailReadMetadataFromXML      (131012, "UTIL", 		"Failed to read the TS groups from the %s XML file."),
	util_FailExpTSGroup               (131013, "UTIL", 		"Failed to export TS groups in the XML file. %s"),
	util_FailImpTSGroup               (131014, "UTIL", 		"Failed to import TS groups from the XML file. %s"),
	util_WriteTSGroupsToDB            (131015, "UTIL", 		"Wtite TS groups to the %s database."),
	util_FailWriteTSGroupToDB         (131016, "UTIL", 		"Failed to wtite the %s TS group to the database."),
	util_SuccessWriteMetadataToDB     (131017, "UTIL", 		"Successful to write the TS groups into the database."),
	util_IncompleteWriteMetadataToDB  (131018, "UTIL", 		"Incompleted to write the TS groups into the database."),
	
	//Database messages
	db_ConnectToDB                    (121001, "DB", 			"Connect to the %s Database."),
	db_DisconnectFromDB               (121002, "DB", 			"Disconnect from the %s Database."),
	db_SuccessConnectToDB             (121003, "DB", 			"Successful to connect to the %s Database."),
	db_SuccessDisconnectFromDB        (121004, "DB", 			"Successful to disconnect from the %s Database."),
	db_FailConnectToDB                (121005, "DB", 			"Failed to connect to the %s Database."),
	db_FailDisconnectFromDB           (121006, "DB", 			"Failed to disconnect from the %s Database."),
	db_OpentheDB                      (121001, "DB", 			"Open the %s Database."),
	db_ClosetheDB                     (121001, "DB", 			"Close to the %s Database."),

	/**
	 * Application level messages
	 */
	//DECODES messages
	decd_FailDecodeData               (211001, "DECD", 		"Fail to decode the raw DCP message."),
	decd_HasDuplicateData             (211002, "DECD", 		"Raw message has the duplicate data.");

	
	private int msgCode;
	private String msgModule;
	private String msgDesc;
	
	public enum ShowMode {_displaying, _warning, _error, _exception};
	
	/**
	 * Constructor
	 * 
	 * @param msgCode
	 * @param msgModule
	 * @param msgDesc
	 */
	AppMessages(int msgCode, String msgModule, String msgDesc)
	{
		this.msgCode = msgCode;
		this.msgModule = msgModule;
		this.msgDesc = msgDesc;
	}
	
	/**
	 * Overload constructor
	 */
	AppMessages()
	{
		this.msgCode = 999999;
		this.msgModule = "---";
		this.msgDesc = "Unknown";
	}

	public int getMsgCode() { return msgCode; }
	
	public String getMsgModule() { return msgModule; }
	
	public String getMsgDescription() { return msgDesc; }
	
	public String showMessage() {
		return showMessage(AppMessages.ShowMode._displaying);
	}
	
	public String showMessage(ShowMode showMode) {
		if (showMode == null) 
			showMode = AppMessages.ShowMode._displaying;
		
		switch (showMode) {
			case _displaying: { return getMsgDescription(); }
			case _warning		: { return getMsgModule()+"-"+getMsgCode()+": "+getMsgDescription(); }
			case _error			: { return getMsgModule()+"-"+getMsgCode()+": "+getMsgDescription(); }
			case _exception : { return getMsgDescription(); }
			default					: { return getMsgDescription(); }
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AppMessages appMsg = sys_Unknown;
		
		System.out.println("Test for application error message");
		System.out.println(appMsg.showMessage());
		
		for(AppMessages msgItem: AppMessages.values()) {
			try {
			  System.out.printf(msgItem.showMessage() + "%n", msgItem);
			  System.out.printf(msgItem.showMessage(AppMessages.ShowMode._error) + "%n", msgItem);
			}
			catch (Exception E) {
				System.out.println(E.toString());
			}
		}
	}


}
