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

package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("/")
public final class RestServices extends ResourceConfig
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RestServices.class);

	;

	public RestServices(@Context ServletContext servletContext)
	{
		LOGGER.debug("Initializing odcsapi RestServices.");
		packages("org.opendcs.odcsapi");
		setupSwagger(servletContext);
	}

	private void setupSwagger(ServletContext servletContext)
	{
		Set<String> resourcePackages = new HashSet<>();
		resourcePackages.add("org.opendcs.odcsapi");
		List<Server> servers = new ArrayList<>();
		String contextPath = servletContext.getContextPath();
		servers.add(new Server().url(contextPath));
		OpenAPI openAPI = new OpenAPI();
		openAPI.setServers(servers);
		SwaggerConfiguration swaggerConfig = new SwaggerConfiguration();
		swaggerConfig.setResourcePackages(resourcePackages);
		swaggerConfig.setOpenAPI(openAPI);
		OpenApiResource openApiResource = new OpenApiResource();
		openApiResource.setOpenApiConfiguration(swaggerConfig);
		register(openApiResource);
	}
}
