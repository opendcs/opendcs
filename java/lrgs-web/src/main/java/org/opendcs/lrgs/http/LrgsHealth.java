/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package org.opendcs.lrgs.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.lrgsmain.LrgsMain;

@Path("/health")
public class LrgsHealth
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    @Context
    ServletContext servletContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get()
    {
        log.info("Sending Health status.");
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        if (lrgs != null && lrgs.getDdsServer().statusProvider.getStatusSnapshot().isUsable)
        {
            return Response.ok("\"Active\"").build();
        }
        else
        {
            return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                           .entity("\"Inactive\"")
                           .build();
        }
    }
}
