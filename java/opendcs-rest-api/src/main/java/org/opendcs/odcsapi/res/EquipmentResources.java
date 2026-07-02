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

import java.util.List;

import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.odcsapi.beans.ApiEquipmentModel;
import org.opendcs.odcsapi.beans.ApiEquipmentModelRef;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class EquipmentResources extends OpenDcsResource
{
    private static final WebAppException UNABLE_TO_GET_EQUIPMENT_DAO =
            new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                "No Equipment Model DAO available.");

    @GET
    @Path("equipmentrefs")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Returns a list of equipment model references suitable for table and pick-list views.",
            description = "Example:\n\n    http://localhost:8080/odcsapi/equipmentrefs",
            operationId = "getequipmentrefs",
            tags = {"REST - DECODES Equipment Model Records"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    array = @ArraySchema(schema = @Schema(implementation = ApiEquipmentModelRef.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response getEquipmentRefs() throws WebAppException
    {
        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(EquipmentModelDao.class)
                              .orElseThrow(() -> UNABLE_TO_GET_EQUIPMENT_DAO);
            final List<EquipmentModel> models = dao.getEquipmentModels(tx, -1, -1);
            return Response.ok().entity(models.stream().map(EquipmentResources::mapRef).toList()).build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                      "Unable to retrieve equipment model references.", ex);
        }
    }

    @GET
    @Path("equipment")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Retrieve a single equipment model by its ID.",
            description = "Example:\n\n    http://localhost:8080/odcsapi/equipment?equipmentid=1",
            operationId = "getequipment",
            tags = {"REST - DECODES Equipment Model Records"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ApiEquipmentModel.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Missing required equipmentid parameter"),
                    @ApiResponse(responseCode = "404", description = "Not Found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response getEquipment(
            @Parameter(description = "Equipment Model ID", required = true, example = "1",
                    schema = @Schema(implementation = Long.class))
            @QueryParam("equipmentid") Long equipmentId) throws WebAppException
    {
        if (equipmentId == null)
        {
            throw new MissingParameterException("Missing required equipmentid parameter.");
        }

        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(EquipmentModelDao.class)
                              .orElseThrow(() -> UNABLE_TO_GET_EQUIPMENT_DAO);
            final EquipmentModel em = dao.getEquipmentModel(tx, DbKey.createDbKey(equipmentId))
                                         .orElseThrow(() -> new DatabaseItemNotFoundException(
                                                 "No equipment model with ID: " + equipmentId));
            return Response.ok().entity(mapFull(em)).build();
        }
        catch (DatabaseItemNotFoundException ex)
        {
            throw ex;
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                      "Unable to retrieve equipment model.", ex);
        }
    }

    @POST
    @Path("equipment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Create or overwrite an equipment model.",
            description = "Omit equipmentId to create a new record. Include an existing equipmentId to overwrite it.",
            operationId = "postequipment",
            tags = {"REST - DECODES Equipment Model Records"},
            requestBody = @RequestBody(
                    description = "Equipment Model",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = ApiEquipmentModel.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully stored equipment model",
                            content = @Content(schema = @Schema(implementation = ApiEquipmentModel.class),
                                    mediaType = MediaType.APPLICATION_JSON)),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response postEquipment(ApiEquipmentModel apiEquipmentModel) throws WebAppException
    {
        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(EquipmentModelDao.class)
                              .orElseThrow(() -> UNABLE_TO_GET_EQUIPMENT_DAO);
            final EquipmentModel saved = dao.saveEquipmentModel(tx, map(apiEquipmentModel));
            tx.commit();
            return Response.status(Response.Status.CREATED).entity(mapFull(saved)).build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                      "Unable to save equipment model.", ex);
        }
    }

    @DELETE
    @Path("equipment")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Delete an equipment model by its ID.",
            description = "Required argument equipmentid must be passed as a query parameter.",
            operationId = "deleteequipment",
            tags = {"REST - DECODES Equipment Model Records"},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted equipment model"),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Missing required equipmentid parameter"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response deleteEquipment(
            @Parameter(description = "Equipment Model ID", required = true, example = "1",
                    schema = @Schema(implementation = Long.class))
            @QueryParam("equipmentid") Long equipmentId) throws WebAppException
    {
        if (equipmentId == null)
        {
            throw new MissingParameterException("Missing required equipmentid parameter.");
        }

        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(EquipmentModelDao.class)
                              .orElseThrow(() -> UNABLE_TO_GET_EQUIPMENT_DAO);
            dao.deleteEquipmentModel(tx, DbKey.createDbKey(equipmentId));
            tx.commit();
            return Response.noContent().build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                      "Unable to delete equipment model.", ex);
        }
    }

    static ApiEquipmentModelRef mapRef(EquipmentModel em)
    {
        return populateRef(new ApiEquipmentModelRef(), em);
    }

    static ApiEquipmentModel mapFull(EquipmentModel em)
    {
        ApiEquipmentModel api = populateRef(new ApiEquipmentModel(), em);
        api.setProperties(em.properties);
        return api;
    }

    private static <T extends ApiEquipmentModelRef> T populateRef(T ref, EquipmentModel em)
    {
        if (em.getId() != null && !em.getId().isNull())
        {
            ref.setEquipmentId(em.getId().getValue());
        }
        ref.setName(em.name);
        ref.setEquipmentType(em.equipmentType);
        ref.setCompany(em.company);
        ref.setModel(em.model);
        ref.setDescription(em.description);
        return ref;
    }

    static EquipmentModel map(ApiEquipmentModel api) throws WebAppException
    {
        EquipmentModel em = new EquipmentModel();
        if (api.getEquipmentId() != null)
        {
            try
            {
                em.setId(DbKey.createDbKey(api.getEquipmentId()));
            }
            catch (DatabaseException ex)
            {
                throw new WebAppException(Response.Status.BAD_REQUEST.getStatusCode(),
                                          "Invalid equipmentId: " + api.getEquipmentId(), ex);
            }
        }
        em.name = api.getName();
        em.equipmentType = api.getEquipmentType();
        em.company = api.getCompany();
        em.model = api.getModel();
        em.description = api.getDescription();
        if (api.getProperties() != null)
        {
            em.properties = api.getProperties();
        }
        return em;
    }
}
