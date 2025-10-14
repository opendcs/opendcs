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
package ilex.util;

/**
* FailureException indicates a failed application-level operation.
* Throwing will cause a E_FAILURE log event.
*/
public class FailureException extends IlexException
{
	/**
	* Constructor.
	* @param msg the message.
	*/
	public FailureException( String msg )
	{
		super(msg);
	}

	public FailureException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
}

