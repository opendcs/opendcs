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

package org.opendcs.odcsapi.beans;

public class ApiTokenPosition
{
	private int start=0;
	private int end=0;
	
	public ApiTokenPosition()
	{
	}
	
	public ApiTokenPosition(int start, int end)
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * @return start position of the token
	 */
	public int getStart()
	{
		return start;
	}

	/**
	 * @return character position after the end of the token
	 */
	public int getEnd()
	{
		return end;
	}
	
	public void setStart(int start)
	{
		this.start = start;
	}

	public void setEnd(int end)
	{
		this.end = end;
	}

}
