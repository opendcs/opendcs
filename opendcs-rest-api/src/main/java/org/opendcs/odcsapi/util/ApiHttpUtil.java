/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

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

	public static Response createResponse(String message, int status)
	{
		//return Response.ok(message).header("X-Content-Type-Options", "nosniff").header("bbbb", "bbbb").status(status).build();
		return Response.status(status).entity(message)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff").build();
	}
	
	public static Response createResponseWithHeaders(Object obj, int status, ArrayList<String[]> headers)
	{
		ResponseBuilder rb = Response.status(status).entity(obj)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff");
		for (int x = 0; x < headers.size(); x++)
		{
			String[] curHeader = headers.get(x);
			rb.header(curHeader[0], curHeader[1]);
		}
		Response finalResponse = rb.build();
		return finalResponse;
				
		//		.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken.getToken())
		//return Response.status(HttpServletResponse.SC_OK).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken.getToken())
				//	.entity(ret).build();
	}
}
