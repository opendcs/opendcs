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

package decodes.comp;

import decodes.comp.ComputationException;

/**
 * Thrown when parsing could not succeed for data necessary to
 * execute a computation. E.g., a bad RDB Rating File.
 */
public class ComputationParseException extends ComputationException
{
	/** Construct new object. */
	public ComputationParseException( String msg )
	{
		super(msg);
	}
}
