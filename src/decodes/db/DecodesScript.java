/*
*  $Id$
*/
package decodes.db;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import decodes.decoder.DecoderException;
import decodes.decoder.DataOperations;
import decodes.decoder.EndOfDataException;
import decodes.decoder.FieldParseException;
import decodes.decoder.SwitchFormatException;
import decodes.decoder.EndlessLoopException;
import decodes.util.DecodesSettings;
import ilex.util.Logger;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;

/**
 * This class encapsulates information about a decoding script.
 */
public class DecodesScript extends IdDatabaseObject
{
	// _id is stored in the IdDatabaseObject superclass.

	/**
	 * Name of this script. Must be unique within a configuration.
	 */
	public String scriptName;

	/** Script type. The only type thus far implemented is 'DECODES'.*/
	public String scriptType;

	/** Links */
	public PlatformConfig platformConfig;

	/**
	* This stores references to the ScriptSensors.  This data member will
	* never be null.  Note that the index
	* of each element of this Vector does not necessarily correspond with
	* the sensor number.
	*/
	public Vector<ScriptSensor> scriptSensors;

	/** The format statments that make up the executable script.*/
	public Vector<FormatStatement> formatStatements;

	/**
	  Data Order for this script is Ascending, Descending or Undefined.
	  Use values in Constants.java.
	  Default is Undefined, meaning to use the value in Equipment Model.
	*/
	private char dataOrder;

	// Executable data
	private boolean _prepared;

	// Format Label mode
	private boolean labelIsCaseSensitive = false;
	
	/** The GUI will set this to true so that raw data positions and samples are tracked. */
	public static boolean trackDecoding = false;
	
	/** if (trackDecoding) then this will store decoded samples after execution. */
	private ArrayList<DecodedSample> decodedSamples = null;
	
	/**
	 * Constructor
	 * @param name Name of this script.
	 */
	public DecodesScript(String name)
	{
		super(); // sets _id to Constants.undefinedId;

		DecodesSettings settings = DecodesSettings.instance();
		if ( settings.decodesFormatLabelMode.equals("case-sensitive" ) )
			labelIsCaseSensitive = true;	
		scriptName = name;
		scriptType = Constants.scriptTypeDecodes;
		platformConfig = null;
		scriptSensors = new Vector<ScriptSensor>();
		formatStatements = new Vector<FormatStatement>();
		dataOrder = Constants.dataOrderUndefined;
		_prepared = false;
	}

	/**
	 * Construct with the owner PlatformConfig and a name.
	 * @param platformConfig The config that owns this script.
	 * @param name the name of the script.
	 */
	public DecodesScript(PlatformConfig platformConfig, String name)
	{
		this(name);
		this.platformConfig = platformConfig;
	}

	/**
	* Get a ScriptSensor by sensor number.
	* If no sensor with a matching number is found, this returns null.
	 * @param sensorNumber the sensor number
	 * @return the ScriptSensor or null if not found.
	 */
	public ScriptSensor getScriptSensor(int sensorNumber)
	{
		for(Iterator<ScriptSensor> it = scriptSensors.iterator(); it.hasNext(); )
		{
			ScriptSensor ss = it.next();
			if (ss.sensorNumber == sensorNumber)
				return ss;
		}
		return null;
	}

	/**
	 *  Adds a script sensor at the correct position in the array.
	 * @param newss the new script sensor to add.
	 */
	public void addScriptSensor(ScriptSensor newss)
	{
		ScriptSensor ss = getScriptSensor(newss.sensorNumber);
		if (ss != null)
			scriptSensors.remove(ss);
		for(int i=0; i<scriptSensors.size(); i++)
		{
			ss = scriptSensors.elementAt(i);
			if (ss.sensorNumber > newss.sensorNumber)
			{
				scriptSensors.insertElementAt(newss, i);
				return;
			}
		}
		scriptSensors.add(newss);
	}

	/**
	  Returns the data order for this script (one of values defined
	  in Constants.java.
	 * @return the data order for this script
	 */
	public char getDataOrder() { return dataOrder; }

	/**
	  Sets the data order for this script (must one of values defined
	  in Constants.java.
	 * @param order the data order.
	 */
	public void setDataOrder(char order) { dataOrder = order; }

	/**
	* This compares two DecodesScripts, and returns true if they can be
	* considered equal.  Note that this doesn't check the SQL database
	* ID number.  Two DecodesScripts are considered equal iff:
	* <ul>
	*   <li>the scriptName's are the same,</li>
	*   <li>the scriptType's are the same,</li>
	*   <li>they have the same number of scriptSensors,</li>
	*   <li>they have the same number of formatStatements,</li>
	*   <li>
	*	 each ScriptSensor of one is "equal" to the corresponding
	*	 ScriptSensor of the other, and
	*   </li>
	*   <li>
	*	 each FormatStatement of one is "equal" to the corresponding
	*	 FormatStatement of the other.
	*   </li>
	*   <li></li>
	* </ul>
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object ob)
	{
		if (!(ob instanceof DecodesScript))
			return false;
		DecodesScript ds = (DecodesScript)ob;
		if (this == ds)
			return true;
		if (!scriptName.equals(ds.scriptName)
		 || !scriptType.equals(ds.scriptType)
		 || scriptSensors.size() != ds.scriptSensors.size()
		 || formatStatements.size() != ds.formatStatements.size()
		 || dataOrder != ds.dataOrder)
			return false;
		for (int i = 0; i < scriptSensors.size(); i++)
		{
			ScriptSensor ss1 = (ScriptSensor) scriptSensors.elementAt(i);
			ScriptSensor ss2 = (ScriptSensor) ds.scriptSensors.elementAt(i);
			if (!ss1.equals(ss2)) return false;
		}
		for (int i = 0; i < formatStatements.size(); i++)
		{
			FormatStatement fs1 =
				(FormatStatement) formatStatements.elementAt(i);
			FormatStatement fs2 =
				(FormatStatement) ds.formatStatements.elementAt(i);
			if (!fs1.equals(fs2)) return false;
		}
		return true;
	}

	/**
	* This is inherited from the DatabaseObject interface.  This always
	* returns "DecodesScript".
	*/
	public String getObjectType() {
		return "DecodesScript";
	}

	/**
	* Called from PlatformConfig.copy(), makes a new copy of this script
	* for inclusion in the new config.
	 * @param newPc The PlatformConfig that is to own the copy
	 * @return the new DecodesScript object
	 */
	public DecodesScript copy(PlatformConfig newPc)
	{
		DecodesScript ret = noIdCopy(newPc);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.
		return ret;
	}

	/**
	* Makes a copy of this script but without the ID.
	* for inclusion in the new config.
	 * @param newPc The PlatformConfig that is to own the copy
	 * @return the new DecodesScript object
	 */
	public DecodesScript noIdCopy(PlatformConfig newPc)
	{
		DecodesScript ret = new DecodesScript(newPc, scriptName);
		ret.scriptType = scriptType;
		ret.dataOrder = dataOrder;
		for(Iterator<ScriptSensor> it = scriptSensors.iterator(); it.hasNext(); )
		{
			ScriptSensor ss = it.next();
			ScriptSensor newSs = new ScriptSensor(ret, ss.sensorNumber);
			//newSs.unitConverterId = ss.unitConverterId;
			if (ss.rawConverter != null)
				newSs.rawConverter = ss.rawConverter.copy();
			//newSs.rawConverter = ss.rawConverter;
			ret.scriptSensors.add(newSs);
		}
		for(Iterator<FormatStatement> it = formatStatements.iterator(); it.hasNext(); )
		{
			FormatStatement fs = it.next();
			FormatStatement newFs = new FormatStatement(ret, fs.sequenceNum);
			newFs.label = fs.label;
			newFs.format = fs.format;
			ret.formatStatements.add(newFs);
		}
		return ret;
	}

	/**
	From DatabaseObject interface, prepare script sensors and format
	statements contained in this script.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		for(Iterator<ScriptSensor> it = scriptSensors.iterator(); it.hasNext(); )
		{
			ScriptSensor ss = it.next();
			ss.prepareForExec();
		}

		/*
		  Need to concatenate the format strings for contiguous format
		  statements that have the same label.
		  The 'fullFormat' holds the concatenated format strings. This
		  will be null for format statements that were added to the end
		  of a previous format.
		*/
		// Initially, set fullFormat to null for all statements.
		for(Iterator<FormatStatement> it = formatStatements.iterator(); it.hasNext(); )
		{
			FormatStatement fs = it.next();
			fs.fullFormat = null;
		}

		// Loop through, doing concatenation.
		FormatStatement execFmt = null;
		for(Iterator<FormatStatement> it = formatStatements.iterator(); it.hasNext(); )
		{
			FormatStatement fs = it.next();
			if (execFmt == null)
			{
				execFmt = fs; // starting new format
				execFmt.fullFormat = execFmt.format;
			}
			else 
			{
				if ((labelIsCaseSensitive && execFmt.label.equals(fs.label))
				 || (!labelIsCaseSensitive && execFmt.label.equalsIgnoreCase(fs.label)))
				{
					// concat this one to previous
					execFmt.fullFormat = execFmt.fullFormat + fs.format;
					fs.fullFormat = null;
				}
				else // labels different, finish-off execFmt & start new one.
				{
					execFmt.prepareForExec();
					execFmt = fs;
					execFmt.fullFormat = execFmt.format;
				}
			}
		}
		if (execFmt != null)
			execFmt.prepareForExec();
		_prepared = true;
	}

	/** From DatabaseObject interface.
	 * @return true if script has been prepared previous.
	 */
	public boolean isPrepared()
	{
			return _prepared;
	}

	/** From DatabaseObject interface; this does nothing.  */
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject's read() method.
	* This does nothing, since the I/O for this class is handled by
	* PlatformConfig and TransportMedium.
	*/
	public void read()
		throws DatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject's write() method.
	* This does nothing, since the I/O for this class is handled by
	* PlatformConfig and TransportMedium.
	*/
	public void write()
		throws DatabaseException
	{
	}

	/**
	* @return the format statement with the requested label, or null
	* if label-not-found.
	*/
	public FormatStatement getFormatStatement(String label)
	{
		for(Iterator<FormatStatement> it = formatStatements.iterator(); it.hasNext(); )
		{
			FormatStatement fs = it.next();
			if ( !labelIsCaseSensitive  ) {
				if ( label.equalsIgnoreCase(fs.label))
			  		return fs;
			} else {	
				if (label.equals(fs.label))
					return fs;
			}
		}
		return null;
	}

	/** 
	 * Executes this decodes script on the passed message data.
	 * @param rawmsg the RawMessage to decode
	 * @return the DecodedMessage containing the raw message and decoded time series.
	 */
	public DecodedMessage decodeMessage(RawMessage rawmsg)
		throws DecoderException, UnknownPlatformException,
			IncompleteDatabaseException, InvalidDatabaseException
	{
		if (!isPrepared())
			prepareForExec();

		if (trackDecoding)
			decodedSamples = new ArrayList<DecodedSample>();
		
		if (formatStatements.size() == 0)
			throw new DecoderException("No format statements");
		
		FormatStatement fs = formatStatements.elementAt(0);
		DecodedMessage decmsg = new DecodedMessage(rawmsg);
		DataOperations dops = new DataOperations(rawmsg);
		while(fs != null)
		{
			try
			{
				fs.execute(dops, decmsg);
				fs = null;
			}
			catch(EndOfDataException ex)
			{
				Logger.instance().log(Logger.E_DEBUG1,
				"Format statements attempted to read past the end of message.");
				fs = null;
			}
			catch(SwitchFormatException ex)
			{
				Logger.instance().debug3(ex.toString());
				fs = ex.getNewFormat();
			}
			catch(FieldParseException ex)
			{
				Logger.instance().log(Logger.E_DEBUG1, ex.toString());
				fs = getFormatStatement("ERROR");
				if (fs == null)
				{
					Logger.instance().log(Logger.E_WARNING, 
						"No ERROR statement, terminating script: "
						+ ex.toString());
					throw ex;
				}
			}
			catch(EndlessLoopException ex )
			{
				Logger.instance().log(Logger.E_WARNING,
						"Platform Config: "+ platformConfig.getName()+", script <"+scriptName+"> in endless loop  -- terminated.");
				throw new DecoderException("Platform Config: "+ platformConfig.getName()+", script <"+scriptName+"> in endless loop  -- terminated.");
			}
			// All other decoding exception will cause failure.
		}
		decmsg.finishMessage();
		decmsg.applyInitialEuConversions();
		return decmsg;
	}
	
	/**
	 * Called from a DECODES operation after a sample is successfully decoded.
	 * @param decodedSample structure containing location within raw message, decoded, data, etc.
	 */
	public void addDecodedSample(DecodedSample decodedSample)
	{
		if (decodedSamples != null)
			decodedSamples.add(decodedSample);
	}

	/**
	 * Get the list of decoded samples populated after a successful decode.
	 * @return the list of decoded samples.
	 */
	public ArrayList<DecodedSample> getDecodedSamples()
	{
		return decodedSamples;
	}
	
	/**
	 * Parse the header type out of the script type
	 * @return the header type or null if none defined.
	 */
	public String getHeaderType()
	{
		int idx = scriptType.indexOf(':');
		if (idx < 0 || scriptType.length() <= idx+1)
			return null;
		return scriptType.substring(idx+1);
	}
}
