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

package org.opendcs.odcsapi.dao;

public class DbException extends Exception
{
	private static final long serialVersionUID = 4552935582551955795L;
	private String module = null;
	private Exception cause = null;
	
	public DbException(String msg)
	{
		super(msg);
	}
	
	public DbException(String module, Exception cause, String msg)
	{
		this(msg);
		this.module = module;
		this.cause = cause;
	}

	public String getModule()
	{
		return module;
	}

	public Exception getCause()
	{
		return cause;
	}
}
