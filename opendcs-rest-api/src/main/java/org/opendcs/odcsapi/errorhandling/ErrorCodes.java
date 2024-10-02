/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.errorhandling;

import javax.servlet.http.HttpServletResponse;

public class ErrorCodes
{
	
	/** username/password authentication failed */
	public static final int AUTH_FAILED = HttpServletResponse.SC_FORBIDDEN;
	
	/** Client requests object with ID that is not in the database */
	public static final int NO_SUCH_OBJECT = HttpServletResponse.SC_GONE;
	
	/** Missing required ID arg */
	public static final int MISSING_ID = HttpServletResponse.SC_NOT_ACCEPTABLE;
	
	/** Unexplained exception during database I/O */
	public static final int DATABASE_ERROR = HttpServletResponse.SC_BAD_REQUEST; //This was 500, but should not be sending 500 back to the end user. "Bad Request" is probably not the proper error code here.
	
	public static final int NOT_ALLOWED = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	
	public static final int BAD_CONFIG = HttpServletResponse.SC_PRECONDITION_FAILED;
	
	public static final int IO_ERROR = HttpServletResponse.SC_CONFLICT;
}
