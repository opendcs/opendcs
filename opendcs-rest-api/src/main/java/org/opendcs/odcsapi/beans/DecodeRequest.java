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

/**
 * Used for POST decode payload. Contains the ApiRawMessage and the ApiPlatformConfig
 * to use for decoding.
 * @author mmaloney
 *
 */
public class DecodeRequest
{
	ApiRawMessage rawmsg = null;
	ApiPlatformConfig config = null;
	
	public ApiPlatformConfig getConfig()
	{
		return config;
	}
	public void setConfig(ApiPlatformConfig config)
	{
		this.config = config;
	}
	public ApiRawMessage getRawmsg()
	{
		return rawmsg;
	}
	public void setRawmsg(ApiRawMessage rawmsg)
	{
		this.rawmsg = rawmsg;
	}

}
