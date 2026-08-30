package org.opendcs.lrgs.http.dds;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.opendcs.data.goes.SpaceCraft;
import org.opendcs.lrgs.dds.DdsSession;
import org.opendcs.lrgs.http.dto.DataSource;
import org.opendcs.lrgs.http.dto.DcpMessages;
import org.opendcs.lrgs.http.dto.GoesMessage;
import org.opendcs.lrgs.messages.MessageRetrieval;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import lrgs.common.ArchiveUnavailableException;
import lrgs.common.DcpAddress;
import lrgs.common.EndOfArchiveException;
import lrgs.common.SearchCriteria;
import lrgs.common.SearchSyntaxException;
import lrgs.common.SearchTimeoutException;
import lrgs.common.UntilReachedException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;
import ilex.util.IDateFormat;

/** DDS-over-HTTP resources served by an LRGS. */
@Path("/dds")
@UseDdsSession
public class DdsHttp
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String MESSAGE_RETRIEVE_FAILED = "\"Failed to get messages\"";
    private static final String INACTIVE = "\"Inactive\"";

    private static final BiFunction<List<GoesMessage>, Exception, Response> handleArchiveError =
        (messages, ex) -> messages.isEmpty()
            ? Response.noContent().header("reason", ex.getMessage()).header("Retry-After", "10").build()
            : Response.ok().entity(envelope(messages)).build();

    @Context
    ServletContext servletContext;

    @GET
    @Path("/data/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNext(@Context HttpServletRequest request)
    {
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        HttpSession session = request.getSession();
        DdsSession ddsSession = (DdsSession)session.getAttribute(UseDdsSession.KEY);
        try (MDCCloseable diagId = MDC.putCloseable("trace-id", UUID.randomUUID().toString()))
        {
            var retriever = ddsSession.msgRetriever();
            if (retriever == null)
                return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE).entity(INACTIVE).build();
            var result = MessageRetrieval.getMessages(retriever, lrgs, 1000);
            return switch (result.ex())
            {
                case UntilReachedException ur -> handleArchiveError.apply(result.messages(), ur);
                case SearchTimeoutException st -> handleArchiveError.apply(result.messages(), st);
                case EndOfArchiveException ea -> handleArchiveError.apply(result.messages(), ea);
                case null -> Response.ok().entity(envelope(result.messages())).build();
                default -> Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                    .entity(MESSAGE_RETRIEVE_FAILED).build();
            };
        }
    }

    @GET
    @Path("/data/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryData(@Context HttpServletRequest request,
                              @QueryParam("dcpName") List<String> dcpNames,
                              @QueryParam("dcpAddress") List<String> dcpAddresses,
                              @QueryParam("since") String since,
                              @QueryParam("until") String until,
                              @QueryParam("ascending") boolean ascending,
                              @QueryParam("spacecraft") SpaceCraft spaceCraft,
                              @QueryParam("source") List<String> sources,
                              @QueryParam("single") boolean single)
    {
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        try
        {
            DdsSession ddsSession = (DdsSession)request.getSession().getAttribute(UseDdsSession.KEY);
            var retriever = ddsSession.msgRetriever();
            SearchCriteria criteria = new SearchCriteria();
            criteria.clear();
            criteria.DcpNames.addAll(dcpNames == null ? List.of() : dcpNames);
            criteria.ExplicitDcpAddrs.addAll((dcpAddresses == null ? List.<String>of() : dcpAddresses)
                .stream().map(DcpAddress::new).toList());
            criteria.setAscendingTimeOnly(ascending);
            criteria.single = single;
            criteria.setLrgsUntil(normalizeDateTime(until));
            criteria.setLrgsSince(normalizeDateTime(since));
            if (spaceCraft != null)
                criteria.spacecraft = spaceCraft.toChar();
            retriever.setSearchCriteria(criteria);

            var result = MessageRetrieval.getMessages(retriever, lrgs, Integer.MAX_VALUE);
            if (result.ex() != null)
                result = MessageRetrieval.getMessages(retriever, lrgs, Integer.MAX_VALUE);
            List<GoesMessage> filtered = filterSources(result.messages(), sources);
            return switch (result.ex())
            {
                case UntilReachedException ur -> handleArchiveError.apply(filtered, ur);
                case SearchTimeoutException st -> handleArchiveError.apply(filtered, st);
                case EndOfArchiveException ea -> handleArchiveError.apply(filtered, ea);
                case null -> filtered.isEmpty() ? Response.noContent().build()
                    : Response.ok().entity(envelope(filtered)).build();
                default -> Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                    .entity(MESSAGE_RETRIEVE_FAILED).build();
            };
        }
        catch (IOException | SearchSyntaxException | ArchiveUnavailableException ex)
        {
            log.error("Cannot get messages", ex);
            return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                .entity(MESSAGE_RETRIEVE_FAILED).build();
        }
    }

    private static String normalizeDateTime(String value)
    {
        if (value == null || value.isBlank() || !value.contains("T"))
            return value;
        return IDateFormat.toString(Date.from(Instant.parse(value)), false);
    }

    @GET
    @Path("/data/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSummary(@Context HttpServletRequest request,
                               @QueryParam("data-group") String dataGroup)
    {
        if (dataGroup == null || dataGroup.isBlank())
            return Response.status(Response.Status.BAD_REQUEST).entity("\"Missing data-group\"").build();
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        DdsSession session = (DdsSession)request.getSession().getAttribute(UseDdsSession.KEY);
        try
        {
            return Response.ok(DdsSummaryService.summarize(session.msgRetriever(), lrgs, dataGroup)).build();
        }
        catch (IOException ex)
        {
            return Response.status(ex.getMessage().startsWith("No such")
                    ? Response.Status.NOT_FOUND : Response.Status.BAD_REQUEST)
                .entity("\"" + ex.getMessage() + "\"").build();
        }
        catch (SearchSyntaxException | ArchiveUnavailableException ex)
        {
            log.error("Cannot summarize data group {}", dataGroup, ex);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(MESSAGE_RETRIEVE_FAILED).build();
        }
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups()
    {
        try
        {
            return Response.ok(DdsSummaryService.listGroups()).build();
        }
        catch (IOException ex)
        {
            log.error("Cannot list DDS data groups", ex);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(MESSAGE_RETRIEVE_FAILED).build();
        }
    }

    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSources()
    {
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        var sources = lrgs.getInputs().stream()
            .filter(input -> input != null && input.getStatusCode() != LrgsInputInterface.DL_DISABLED)
            .map(input -> new DataSource(input.getInputName(), sourceType(input.getInputName())))
            .toList();
        return Response.ok().entity(sources).build();
    }

    private static DcpMessages envelope(List<GoesMessage> messages)
    {
        DataSource source = messages.isEmpty() ? null : messages.getFirst().dataSource();
        return new DcpMessages(messages.size(), messages, source);
    }

    private static List<GoesMessage> filterSources(List<GoesMessage> messages, List<String> sources)
    {
        if (sources == null || sources.isEmpty())
            return messages;
        return messages.stream().filter(message -> sources.stream().anyMatch(source ->
            source.equalsIgnoreCase(message.messageType())
                || source.equalsIgnoreCase(message.dataSource().getName()))).toList();
    }

    private static String sourceType(String inputName)
    {
        String name = inputName.toUpperCase(Locale.ROOT);
        if (name.contains("HRIT")) return "HRIT";
        if (name.contains("DRGS")) return "DRGS";
        if (name.contains("DDS")) return "LRGS";
        if (name.contains("NOAAPORT")) return "NOAPORT";
        if (name.contains("HTTP")) return "WEB";
        return "GOES";
    }
}
