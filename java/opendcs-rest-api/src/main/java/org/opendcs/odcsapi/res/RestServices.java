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

import javax.sql.DataSource;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Context;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

@ApplicationPath("/")
public final class RestServices extends ResourceConfig
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public RestServices(@Context ServletContext servletContext)
    {
        log.debug("Initializing odcsapi RestServices.");
        packages("org.opendcs.odcsapi");
        register(ObjectMapperContextResolver.class);
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
        var components = new Components();

        DataSource dataSource = (DataSource) servletContext.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
        if(dataSource == null)
        {
            throw new IllegalStateException("DataSource not found in ServletContext.");
        }
        var db = OpenDcsDatabaseFactory.createDb(dataSource, "");

        try (var tx = db.newTransaction())
        {
            var umDao = db.getDao(UserManagementDao.class)
                          .orElseThrow(() -> new OpenDcsDataException("No user Management Dao is available."));
            var providers = umDao.getIdentityProviders(tx, -1, -1);
            for (var provider: providers)
            {
                components.addSecuritySchemes(provider.getName(), provider.getSecurityScheme());
            }
        }
        catch (OpenDcsDataException ex)
        {
            throw new IllegalStateException("Unable to configure authorization elements for OpenAPI spec.", ex);
        }

        openAPI.setComponents(components);
        SwaggerConfiguration swaggerConfig = new SwaggerConfiguration();
        swaggerConfig.setResourcePackages(resourcePackages);
        swaggerConfig.setOpenAPI(openAPI);
        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.setOpenApiConfiguration(swaggerConfig);

        register(openApiResource);
    }
}
