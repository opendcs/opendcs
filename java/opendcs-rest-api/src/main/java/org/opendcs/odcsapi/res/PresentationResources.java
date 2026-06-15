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
import java.util.Date;
import java.util.List;
import java.util.Vector;

import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.DataTypeDao;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.odcsapi.beans.ApiPresentationElement;
import org.opendcs.odcsapi.beans.ApiPresentationGroup;
import org.opendcs.odcsapi.beans.ApiPresentationRef;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class PresentationResources extends OpenDcsResource
{
    private static final WebAppException UNABLE_TO_GET_PRESENTATIONGROUP_DAO = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "No Presentation Group DAO available.");

    @Context HttpHeaders httpHeaders;

    @GET
    @Path("presentationrefs")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Returns a list of references to presentation groups suitable for displaying a list",
            tags = {"REST - DECODES Presentation Group Records"},
            operationId = "getpresentationrefs",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = ApiPresentationRef.class)))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response getPresentationRefs() throws WebAppException
    {
        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(PresentationGroupDao.class).orElseThrow(() -> UNABLE_TO_GET_PRESENTATIONGROUP_DAO);
            final var groups = dao.getAll(tx, -1, -1)
                                   .stream()
                                   .map(pg -> mapRef(pg))
                                   .toList();
            return Response.ok().entity(groups).build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                       "Unable to retrieve presentation group refs.", ex);
        }
    }

    static ApiPresentationRef mapRef(PresentationGroup group)
    {
        ApiPresentationRef presRef = new ApiPresentationRef();
        if (group.getId() != null)
        {
            presRef.setGroupId(group.getId().getValue());
        }
        else
        {
            presRef.setGroupId(DbKey.NullKey.getValue());
        }
        presRef.setName(group.groupName);
        presRef.setInheritsFrom(group.inheritsFrom);
        presRef.setProduction(group.isProduction);
        if (group.inheritsFrom != null && !group.inheritsFrom.isEmpty() && group.parent != null)
        {
            presRef.setInheritsFromId(group.parent.getId().getValue());
        }
        else
        {
            presRef.setInheritsFromId(DbKey.NullKey.getValue());
        }
        presRef.setLastModified(group.lastModifyTime);
        return presRef;
    }

    @GET
    @Path("presentation")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "This method returns a JSON representation of a single, complete DECODES Presentation Group record",
            description = "Example: \n \n `http://localhost:8080/odcsapi/presentation?groupid=4` \n \n "
                    + "This method returns a JSON representation of a single, complete DECODES Presentation Group record. "
                    + "The following structure is returned.\n\n"
                    + "**Note**: the optional min and max elements are not always present.",
            tags = {"REST - DECODES Presentation Group Records"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ApiPresentationGroup.class))),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid group ID parameter"),
                    @ApiResponse(responseCode = "404", description = "Presentation group not found"),
                    @ApiResponse(responseCode = "500", description = "Default error sample response")
            }
    )
    public Response getPresentation(@Parameter(description = "presentation group id", required = true, example = "4",
            schema = @Schema(implementation = Long.class))
        @QueryParam("groupid") Long groupId)
            throws WebAppException
    {
        if (groupId == null)
        {
            throw new MissingParameterException("Missing required groupid parameter.");
        }


        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(PresentationGroupDao.class).orElseThrow(() -> UNABLE_TO_GET_PRESENTATIONGROUP_DAO);
            final var group = dao.getById(tx, DbKey.createDbKey(groupId))
                                 .orElseThrow(() -> new DatabaseItemNotFoundException(String.format("Presentation group with ID %s not found", groupId)));
            return Response.ok().entity(map(group)).build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                       "Unable to retrieve presentation group.", ex);
        }
    }

    static ApiPresentationGroup map(PresentationGroup group)
    {
        ApiPresentationGroup presGrp = new ApiPresentationGroup();
        presGrp.setLastModified(group.lastModifyTime);
        presGrp.setName(group.groupName);
        presGrp.setProduction(group.isProduction);
        if (group.parent != null && group.parent.groupName != null && !group.parent.groupName.isEmpty())
        {
            presGrp.setInheritsFrom(group.parent.groupName);
            presGrp.setInheritsFromId(group.parent.getId().getValue());
        }
        if (group.getId() != null)
        {
            presGrp.setGroupId(group.getId().getValue());
        }
        else
        {
            presGrp.setGroupId(DbKey.NullKey.getValue());
        }
        presGrp.setElements(map(group.dataPresentations));
        return presGrp;
    }

    static List<ApiPresentationElement> map(List<DataPresentation> dataPresentations)
    {
        List<ApiPresentationElement> ret = new ArrayList<>();
        for(DataPresentation dp : dataPresentations)
        {
            ApiPresentationElement ape = new ApiPresentationElement();
            ape.setDataTypeCode(dp.getDataType().getCode());
            ape.setDataTypeStd(dp.getDataType().getStandard());
            ape.setFractionalDigits(dp.getMaxDecimals());
            ape.setMax(dp.getMaxValue());
            ape.setMin(dp.getMinValue());
            ape.setUnits(dp.getUnitsAbbr());
            ret.add(ape);
        }
        return ret;
    }

    @POST
    @Path("presentation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Create or Overwrite Existing Decodes Presentation Group",
            description = "It takes a single DECODES "
                    + "Presentation Group in JSON format, as described above for GET.\n\n"
                    + "For creating a new record, leave groupId out of the passed data structure.\n\n"
                    + "For overwriting an existing one, include the groupId that was previously returned. "
                    + "The presentation group in the database is replaced with the one sent.",
            tags = {"REST - DECODES Presentation Group Records"},
            requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = ApiPresentationGroup.class),
                    mediaType = MediaType.APPLICATION_JSON,
                    examples = {
                            @ExampleObject(name = "Basic", value = ResourceExamples.PresentationExamples.BASIC),
                            @ExampleObject(name = "New", value = ResourceExamples.PresentationExamples.NEW),
                            @ExampleObject(name = "Update", value = ResourceExamples.PresentationExamples.UPDATE)
                    }), required = true),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully stored presentation group",
                            content = @Content(schema = @Schema(implementation = ApiPresentationGroup.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response postPresentation(@Parameter(description = "Presentation group data", required = true)
        ApiPresentationGroup presGrp)
            throws WebAppException
    {

        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(PresentationGroupDao.class).orElseThrow(() -> UNABLE_TO_GET_PRESENTATIONGROUP_DAO);
            final var dtDao = db.getDao(DataTypeDao.class).orElseThrow(() -> DatatypeUnitResources.UNABLE_TO_GET_DT_DAO);
            final var group = dao.save(tx, map(tx, dtDao, presGrp));
            return Response.status(Response.Status.CREATED)
                           .entity(map(group))
                           .build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                      "Unable to retrieve presentation group.", ex);
        }
    }

    static PresentationGroup map(DataTransaction tx, DataTypeDao dtDao, ApiPresentationGroup presGrp) throws OpenDcsDataException
    {
        // The inheritsFromId is not present in the target data object, so it is not mapped
        // The inheritsFromId is found internally using the inheritsFrom group name
        PresentationGroup group = new PresentationGroup();
        group.lastModifyTime = presGrp.getLastModified();
        group.groupName = presGrp.getName();
        if (presGrp.getGroupId() != null)
        {
            group.forceSetId(DbKey.createDbKey(presGrp.getGroupId()));
        }
        else
        {
            group.forceSetId(DbKey.NullKey);
        }
        group.isProduction = presGrp.isProduction();
        group.inheritsFrom = presGrp.getInheritsFrom();
        if (presGrp.getInheritsFromId() != null)
        {
            PresentationGroup parentGroup = new PresentationGroup();
            parentGroup.groupName = presGrp.getInheritsFrom();
            parentGroup.forceSetId(DbKey.createDbKey(presGrp.getInheritsFromId()));
            group.parent = parentGroup;
        }
        group.dataPresentations = map(tx, dtDao, presGrp.getElements(), group);
        group.lastModifyTime = new Date();
        return group;
    }

    static Vector<DataPresentation> map(DataTransaction tx, DataTypeDao dtDao, List<ApiPresentationElement> elements, PresentationGroup group)
            throws OpenDcsDataException
    {
        Vector<DataPresentation> ret = new Vector<>();

        for (ApiPresentationElement ape : elements)
        {
            DataPresentation dataPres = new DataPresentation();
            dataPres.setUnitsAbbr(ape.getUnits());
            final var dt = dtDao.lookup(tx, ape.getDataTypeStd(), ape.getDataTypeCode())
                                .orElseGet(() -> new DataType(ape.getDataTypeStd(), ape.getDataTypeCode()));

            dataPres.setDataType(dt);
            dataPres.setMaxDecimals(ape.getFractionalDigits());
            dataPres.setMinValue(ape.getMin());
            dataPres.setMaxValue(ape.getMax());
            dataPres.setGroup(group);
            ret.add(dataPres);
        }
        return ret;
    }

    @DELETE
    @Path("presentation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
    @Operation(
            summary = "Delete Existing Presentation Group",
            description = "Required argument groupid must be passed in the URL.",
            tags = {"REST - DECODES Presentation Group Records"},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Successfully deleted presentation group"),
                    @ApiResponse(responseCode = "400", description = "Missing or invalid group ID parameter"),
                    @ApiResponse(responseCode = "405", description = "Cannot delete the group because it is in use"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public Response deletePresentation(@Parameter(description = "presentation group id", required = true, example = "4",
            schema = @Schema(implementation = Long.class))
        @QueryParam("groupid") Long groupId)
            throws WebAppException
    {
        if (groupId == null)
        {
            throw new MissingParameterException("Missing required groupid parameter.");
        }

        final var db = createDb();
        try (var tx = db.newTransaction())
        {
            final var dao = db.getDao(PresentationGroupDao.class).orElseThrow(() -> UNABLE_TO_GET_PRESENTATIONGROUP_DAO);

            dao.delete(tx, DbKey.createDbKey(groupId));
            return Response.noContent()
                           .entity("Presentation Group with ID " + groupId + " deleted")
                           .build();
        }
        catch (OpenDcsDataException ex)
        {
            throw new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                       "Unable to delete presentation group.", ex);
        }

        // Left for reference when "foreign key conflicts" are handled better.
        // return Response.status(Response.Status.METHOD_NOT_ALLOWED)
        //         .entity(String.format("Cannot delete presentation group %s " +
        //                 "because it is used by the following routing specs: %s", groupId, routeSpecs)).build();

    }
}