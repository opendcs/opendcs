package org.opendcs.odcsapi.res;

import java.sql.SQLException;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.opendcs.odcsapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import opendcs.dao.EnumSqlDao;

/**
 * Provide health checks in the format provided by microprofile-health.
 * 
 * 
 * NOTE: should we ever migrate to a provider (such as Quarkus) we should
 * migrate these to the appropriate annotation based system. For now setting
 * up 3 individual endpoints and using the HeckCheckResponse from the API should be sufficient.
 */
@Path("/health")
@Hidden // not specifically documented.
public final class HealthCheckResources extends OpenDcsResource
{
    @GET
    @Path("/ready")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_GUEST})
    public Response readyCheck()
    {
        try (var conn = this.getDataSource().getConnection())
        {
            var hcr = new HealthCheckResponse("ready", Status.UP, Optional.empty());
            return Response.ok().entity(hcr).build();
        }
        catch (SQLException ex)
        {
            var hcr = new HealthCheckResponse("ready", Status.DOWN, Optional.empty());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(hcr).build();
        }
    }

    @GET
    @Path("/live")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_GUEST})
    public Response liveCheck()
    {
        try
        {
            var db = this.createDb();
            db.getDao(EnumSqlDao.class).orElseThrow();
            var hcr = new HealthCheckResponse("live", Status.UP, Optional.empty());
            return Response.ok().entity(hcr).build();
        }
        catch (Throwable ex)
        {
            var hcr = new HealthCheckResponse("live", Status.DOWN, Optional.empty());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(hcr).build();
        }
    }

    @GET
    @Path("/started")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_GUEST})
    public Response startedCheck()
    {
        var hcr = new HealthCheckResponse("started", Status.UP, Optional.empty());
        return Response.ok().entity(hcr).build();
    }
    
}
