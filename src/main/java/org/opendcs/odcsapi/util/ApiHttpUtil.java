package org.opendcs.odcsapi.util;

import java.util.ArrayList;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import jakarta.servlet.http.HttpServletResponse;

public class ApiHttpUtil {

	
	public static Response createResponse(Object obj)
	{
		//return Response.ok(obj).header("X-Content-Type-Options", "nosniff").header("aaaa", "aaaa").status(HttpServletResponse.SC_OK).build();
		return Response.status(HttpServletResponse.SC_OK).entity(obj)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Origin", "http://192.168.99.62,localhost")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff").build();
	}

	public static Response createResponse(Object obj, int status)
	{
		//return Response.ok(obj).header("X-Content-Type-Options", "nosniff").header("aaaa", "aaaa").status(status).build();
		return Response.status(status).entity(obj)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Origin", "http://192.168.99.62,localhost")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff").build();
	}

	public static Response createResponse(String message)
	{
		//return Response.ok(message).header("X-Content-Type-Options", "nosniff").header("bbbb", "bbbb").status(HttpServletResponse.SC_OK).build();
		return Response.status(HttpServletResponse.SC_OK).entity(message)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Origin", "http://192.168.99.62,localhost")
				.header("Access-Control-Allow-Credentials", "true")
				//.header("Access-Control-Allow-Methods", "POST, GET")
				.header("Access-Control-Allow-Headers", "Content-Type")
				.header("Content-Type", "application/json")
				//.header("Content=Security-Policy", "default-src 'self';")
				.header("X-Content-Type-Options", "nosniff").build();
	}

	public static Response createResponse(String message, int status)
	{
		//return Response.ok(message).header("X-Content-Type-Options", "nosniff").header("bbbb", "bbbb").status(status).build();
		return Response.status(status).entity(message)
				.header("Strict-Transport-Security", "max-age=63072000")
				.header("Access-Control-Allow-Origin", "http://192.168.99.62,localhost")
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
				.header("Access-Control-Allow-Origin", "http://192.168.99.62,localhost")
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
