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

/**
 * $Id: ApiAlgorithmRef.java,v 1.1 2022/11/29 15:05:13 mmaloney Exp $
 * 
 * Open Source Software
 * 
 * $Log: ApiAlgorithmRef.java,v $
 * Revision 1.1  2022/11/29 15:05:13  mmaloney
 * First cut of refactored DAOs and beans to remove dependency on opendcs.jar
 *
 * Revision 1.1.1.1  2022/11/10 15:26:19  cvs
 * odcsapi 1.0.0
 *
 * Revision 1.2  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package org.opendcs.odcsapi.beans;


/**
 * This class holds the info for an algorithm in the on-screen list.
 */
public class ApiAlgorithmRef
{
	private Long algorithmId = null;
	private String algorithmName = "";
	private String execClass = "";
	private int numCompsUsing = 0;
	private String description = "";
	
	public ApiAlgorithmRef()
	{
		
	}
	
	public Long getAlgorithmId()
	{
		return algorithmId;
	}
	public void setAlgorithmId(Long algorithmId)
	{
		this.algorithmId = algorithmId;
	}
	public String getAlgorithmName()
	{
		return algorithmName;
	}
	public void setAlgorithmName(String algorithmName)
	{
		this.algorithmName = algorithmName;
	}
	public String getExecClass()
	{
		return execClass;
	}
	public void setExecClass(String execClass)
	{
		this.execClass = execClass;
	}
	public int getNumCompsUsing()
	{
		return numCompsUsing;
	}
	public void setNumCompsUsing(int numCompsUsing)
	{
		this.numCompsUsing = numCompsUsing;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	
}
