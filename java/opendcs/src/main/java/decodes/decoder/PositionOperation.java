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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
PositionOperation implements the nP operator, which moves the character to
the nth character on the current line.
*/
public class PositionOperation extends DecodesOperation 
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	  Constructor.
	  @param  position the desired position on the line.
	*/
	public PositionOperation(int position)
	{
		// Note 'position' stored as 'repetitions' in super class.
		super(position);
	}

	/** @return type code for this operation. */
	public char getType() { return 'P'; }

	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		log.trace("Positioning to {}", repetitions);
		dd.position(repetitions);
	}
}

