/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

package org.opendcs.odcsapi.util;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.opendcs.odcsapi.beans.Status;

public class ApiHttpUtil {

	
	public static Response createResponse(Object obj)
	{
		//return Response.ok(obj).header("X-Content-Type-Options", "nosniff").header("aaaa", "aaaa").status(HttpServletResponse.SC_OK).build();
		return Response.status(HttpServletResponse.SC_OK).entity(obj)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff").build();
	}

	public static Response createResponse(Object obj, int status)
	{
		return Response.status(status).entity(obj)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				.header("X-Content-Type-Options", "nosniff").build();
	}

	//Need to change calls to this method from DELETE that should return 204 and not a content body.
	//https://github.com/opendcs/rest_api/issues/195
	public static Response createResponse(String message)
	{
		Status status = new Status(message);
		return Response.status(HttpServletResponse.SC_OK).entity(status)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Credentials", "true")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				.header("X-Content-Type-Options", "nosniff").build();
	}
}
