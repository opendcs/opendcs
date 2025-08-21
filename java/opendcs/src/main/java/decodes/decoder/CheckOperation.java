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
DecodesCheckCommand is a DecodesOperation that checks the
current locat
*/
class CheckOperation extends DecodesOperation
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private int numChars;
	private FormatStatement newFormat;
	private char checkFor;
	private String checkString;

	private static final char CHECK_FOR_SIGN = 's';
	private static final char CHECK_FOR_NUMBER = 'n';
	private static final char CHECK_FOR_STRING = '\'';
	private boolean labelIsCaseSensitive = false;

	public char getType() { return 'c'; }

	/**
	  Constructor.
	  @param args string inside the parens
	  @param ds the script.
	*/
	CheckOperation(String args, DecodesScript ds)
		throws ScriptFormatException
	{
		super(1);  // By definition, only 1 repetition.

		numChars = 1;
		checkString = null;

		DecodesSettings settings = DecodesSettings.instance();
		if ( settings.decodesFormatLabelMode.equals("case-sensitive" ) )
			labelIsCaseSensitive = true;

		ArgumentTokenizer tokenizer = new ArgumentTokenizer(args, !labelIsCaseSensitive);

		String what = tokenizer.getNextToken();
		if (what == null || what.length() == 0)
			throw new ScriptFormatException("No target in check operation");
		boolean quoted = tokenizer.wasQuoted();

		String label = tokenizer.getNextToken();
		if (label == null)
			throw new ScriptFormatException("No label in check operation");
		else {
			if ( !labelIsCaseSensitive )
				label = label.toLowerCase();
		}
		newFormat = ds.getFormatStatement(label);
		if (newFormat == null)
			throw new ScriptFormatException("No such format statement '"
				 + label + "'");

		if (quoted)
		{
			checkFor = CHECK_FOR_STRING;
			checkString = what;
		}
		else if (what.toLowerCase().charAt(0) == 's')
		{
			checkFor = CHECK_FOR_SIGN;
		}
		else if (what.toLowerCase().charAt(0) == 'n')
		{
			checkFor = CHECK_FOR_NUMBER;
			numChars = 1;
		}
		else if (Character.isDigit(what.charAt(0)))
		{
			int n = 1;
			while(n < what.length() && Character.isDigit(what.charAt(n)))
				n++;
			numChars = Integer.parseInt(what.substring(0, n));
			if (n >= what.length() || what.toLowerCase().charAt(n) != 'n')
				throw new ScriptFormatException(
					"Illegal target '" + what + "' in check operation");
			checkFor = CHECK_FOR_NUMBER;
		}
		else
			throw new ScriptFormatException(
				"Illegal target '" + what + "' in check operation");
	}


	/**
	  Executes the check.
	  @param dd DataOperations containing message context
	  @param msg DecodedMessage in which to store results
	*/
	public void execute(DataOperations dd, DecodedMessage msg)
		throws DecoderException
	{
		int pos = dd.getBytePos();
		boolean found = false;
		switch(checkFor)
		{
		case CHECK_FOR_NUMBER:
			found = dd.checkNum(numChars);
			log.trace("check(number), pos={}, result={}", pos, found);
			break;
		case CHECK_FOR_SIGN:
			found = dd.checkSign();
			log.trace("check(sign), pos={}, result={}", pos, found);
			break;
		case CHECK_FOR_STRING:
			found = dd.checkString(checkString);
			log.trace("check('{}'), pos={}, result={}", checkString, pos, found);
			break;
		}

		if ( !found )
			throw new SwitchFormatException(newFormat);
	}
}
