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

public class WebAppException 
	extends Exception 
{
	private static final long serialVersionUID = 5143111031434975319L;

	/** HTTP Status Code */
	private Integer status = 0;

	/** detailed error msg */
	private String errMessage = "";

	public WebAppException(Integer status, String errMessage)
	{
		super(errMessage);
		this.status = status;
		this.errMessage = errMessage;
	}

	public WebAppException(Integer status, String errMessage, Throwable throwable)
	{
		super(errMessage, throwable);
		this.status = status;
		this.errMessage = errMessage;
	}

	public WebAppException() { }

	public int getStatus() {
		return status;
	}

	public String getErrMessage() {
		return errMessage;
	}

	public void setErrMessage(String errMessage) { this.errMessage = errMessage; }

	public void setStatus(Integer status)
	{
		this.status = status;
	}

}
