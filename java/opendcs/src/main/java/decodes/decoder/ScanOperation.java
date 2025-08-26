/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.decoder;

import ilex.util.ArgumentTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.util.DecodesSettings;

/**
DecodesScanCommand is a DecodesOperation that scans data for a pattern.
*/
public class ScanOperation extends DecodesOperation 
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private int numChars;
	private FormatStatement newFormat;
	private char scanFor;
	private String scanString;
	private char scanChar;

	private static final char SCAN_FOR_SIGN = 's';
	private static final char SCAN_FOR_NUMBER = 'n';
	private static final char SCAN_FOR_STRING = '\'';
	private static final char SCAN_FOR_CHAR = 'c';
	private static final char SCAN_FOR_LETTER = 'a';
	private static final char SCAN_FOR_PSEUDOBINAY = 'p';
	// Format Label mode
	private boolean labelIsCaseSensitive = false;

	public char getType() { return 's'; }

	/**
	  Constructor.
	  @param  args complete string inside the parens
	  @param  ds the DecodesScript that this operation belongs to
	  @throws ScriptFormatException if syntax error detected in arguments
	*/
	public ScanOperation(String args, DecodesScript ds)
		throws ScriptFormatException
	{
		super(1);  // By definition, only 1 repetition.

		DecodesSettings settings = DecodesSettings.instance();
		if ( settings.decodesFormatLabelMode.equals("case-sensitive" ) )
			labelIsCaseSensitive = true;	
		numChars = 1;
		scanString = null;
		scanChar = (char)0;

		ArgumentTokenizer tokenizer = new ArgumentTokenizer(args, !labelIsCaseSensitive);

		String nstr = tokenizer.getNextToken();
		if (nstr == null)
			throw new ScriptFormatException("Scan operation with no 'n'");
		try { numChars = Integer.parseInt(nstr); }
		catch(NumberFormatException ex)
		{
			throw new ScriptFormatException(
				"Exception integer number-of-chars in scan operation.", ex);
		}

		String what = tokenizer.getNextToken();
		if (what == null)
			throw new ScriptFormatException("No target in scan operation");
		boolean quoted = tokenizer.wasQuoted();

		String label = tokenizer.getNextToken();
		if (label == null)
			throw new ScriptFormatException("No label in check operation");
		newFormat = ds.getFormatStatement(label);
		if (newFormat == null)
			throw new ScriptFormatException("No such format statement '"
				 + label + "'");

		if (quoted)
		{
			if (what.length() == 1)
			{
				scanFor = SCAN_FOR_CHAR;
				scanChar = what.charAt(0);
			}
			else
			{
				scanFor = SCAN_FOR_STRING;
				scanString = what;
			}
		}
		else if (what.toLowerCase().charAt(0) == 's')
		{
			scanFor = SCAN_FOR_SIGN;
		}
		else if (what.toLowerCase().charAt(0) == 'n')
		{
			scanFor = SCAN_FOR_NUMBER;
		}
		else if (what.toLowerCase().charAt(0) == 'a')
		{
			scanFor = SCAN_FOR_LETTER;
		}
		else if (what.toLowerCase().charAt(0) == 'p')
		{
			scanFor = SCAN_FOR_PSEUDOBINAY;
		}
		else
			throw new ScriptFormatException("Illegal target in scan operation");
	}


	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		boolean found = false;
		int n;

		for(n=0; !found  && n < repetitions; n++ ) 
	    	switch(scanFor) 
			{
			case SCAN_FOR_NUMBER:
				found = dd.scanNum(numChars);
				break;

			case SCAN_FOR_STRING:
				log.trace("Scanning {} bytes for '{}' on line {} at position {}",
						  numChars, scanString, dd.getCurrentLine(), dd.getRelBytePos());
				found = dd.scanString(numChars, scanString);
				break;

			case SCAN_FOR_CHAR:
				log.trace("Scanning {} bytes for '{}' on line {} at position {}",
						  numChars, scanChar, dd.getCurrentLine(), dd.getRelBytePos());
				found=dd.scanChar(numChars, (byte)scanChar);
				break;

			case SCAN_FOR_SIGN:
				found=dd.scanSign(numChars);
				break;

			case SCAN_FOR_LETTER:
				found=dd.scanAlpha(numChars);
				break;
				
			case SCAN_FOR_PSEUDOBINAY:				
				log.trace("Scanning data for pseudobinary characters");
				found=dd.scanPseudoBinary(numChars);				
				break;
	    	}
		if (!found)
		{
	        throw new SwitchFormatException(newFormat);
		}
	}
}
