/*
*  $Id$
*/
package decodes.db;

import decodes.decoder.DecodesOperationGroup;
import decodes.decoder.ScriptFormatException;

import ilex.util.Logger;

import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;
import decodes.decoder.DataOperations;

/**
A FormatStatement is a member of a DecodesScript.
When it is prepared for execution, this class implements the basic 
functionality to parse and execute the DECODES format language.
*/
public class FormatStatement extends DatabaseObject
{
	/** The sequence number of this format statement */
	public int sequenceNum;
	/** The label of this format statement */
	public String label;
	/** The text of this format statement */
	public String format;

	// Links
	private DecodesScript decodesScript;

	// Executable links
	private DecodesOperationGroup headDog;
	String fullFormat; // Concatenated statement to convert to dogs.
	
	/**
	  Construct new FormatStatement.
	  @param decodesScript the script that owns this
	  @param sequenceNum the sequence number of this
	*/
	public FormatStatement(DecodesScript decodesScript, int sequenceNum)
	{
		//decodesScriptId = decodesScript.decodesScriptId;
		this.sequenceNum = sequenceNum;
		this.decodesScript = decodesScript;
		label="";
		format="";
		headDog = null;
		fullFormat = null;
	}

	/** @return "FormatStatement" */
	public String getObjectType() { return "FormatStatement"; }

	public boolean equals(Object ob)
	{
		if (!(ob instanceof FormatStatement))
			return false;
		FormatStatement fs = (FormatStatement)ob;
		if (sequenceNum != fs.sequenceNum
		 || !label.equals(fs.label)
		 || !format.equals(fs.format))
			return false;
		return true;
	}

 	/**
 	* Construct DecodesOperationGroup to implement this format statement.
 	*/
	public void prepareForExec()
		throws InvalidDatabaseException
	{
		//if (label != null)
		//	label = label.toLowerCase();
		if (fullFormat == null)
			return;

		try
		{
			headDog = new DecodesOperationGroup(1, fullFormat, decodesScript, 0, this);
			//headDog = new DecodesOperationGroup(1, format, decodesScript);
		}
		catch(ScriptFormatException e)
		{
			throw new InvalidDatabaseException(
				"FormatStatement '" + label + "': " + e);
		}
	}

	/** @return true if previously prepared. */
	public boolean isPrepared()
	{
		return fullFormat == null || headDog != null;
	}

	/** Does nothing. */
    public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

  /**
   * This overrides the DatabaseObject method.
   * This does nothing; I/O for this is handled by the DecodesScript.
   */
	public void read()
		throws DatabaseException
	{
	}

  /**
   * This overrides the DatabaseObject method.
   * This does nothing; I/O for this is handled by the DecodesScript.
   */
	public void write()
		throws DatabaseException
	{
	}

    /**
      Executes this format statement on the passed message data.
      The current decoding context is encapsulated inside DataOperations.
	  @param dops holds copy and context of message being decoded
	  @param dm place decoded data here.
   	*/
    public void execute(DataOperations dops, DecodedMessage dm)
		throws DecoderException
	{
    	Logger.instance().log(Logger.E_DEBUG3, "Executing format '"+label+"'");
    	dops.checkFormatPosition(this);
    	headDog.execute(dops, dm);
	}

	public DecodesScript getDecodesScript()
	{
		return decodesScript;
	}
}
