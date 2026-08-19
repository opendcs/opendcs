package org.opendcs.lrgs.http.dds;

import java.io.IOException;
import java.util.List;
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

@Path("/dds")
@UseDdsSession
public class DdsHttp
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();


    private static final String MESSAGE_RETRIEVE_FAILED = "\"Failed to get messages\"";
    private static final String INACTIVE = "\"Inactive\"";

    private static final BiFunction<List<org.opendcs.lrgs.http.dto.DcpMsg>, Exception, Response> handleArchiveError =
        (messages, ex) -> messages.isEmpty() ?
                          Response.noContent().header("reason", ex.getMessage()).header("Retry-After", "10").build() :
                          Response.ok().entity(messages).build();

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
            var mar = ddsSession.msgRetriever();
            if (mar != null)
            {
                var result = MessageRetrieval.getMessages(mar, lrgs, 1000);
                return switch (result.ex())
                {
                    case UntilReachedException ur -> handleArchiveError.apply(result.messages(), result.ex());
                    case SearchTimeoutException st -> handleArchiveError.apply(result.messages(), result.ex());
                    case EndOfArchiveException ea ->  handleArchiveError.apply(result.messages(), result.ex());
                    case null -> Response.ok().entity(result.messages()).build();
                    default -> Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                                .entity(MESSAGE_RETRIEVE_FAILED)
                                .build();
                };
            }
            else
            {
                return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                            .entity(INACTIVE)
                            .build();
            }
        }
    }

    @GET
    @Path("/data/query")
    public Response queryData(@Context HttpServletRequest request,
                              @QueryParam("dcpName") List<String> dcpNames,
                              @QueryParam("dcpAddress") List<String> dcpAddresses,
                              @QueryParam("since") String since, @QueryParam("until") String until,
                              @QueryParam("ascending") boolean ascending, @QueryParam("spacecraft") SpaceCraft spaceCraft,
                              @QueryParam("source") List<String> sources, @QueryParam("single") boolean single
                              )
    {
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        var session = request.getSession(false);
        log.info("Addresses {}", dcpAddresses.getFirst());
        try
        {
            var ddsSession = (DdsSession)session.getAttribute(UseDdsSession.KEY);
            var mar = ddsSession.msgRetriever();
            final var sc = new SearchCriteria();
            sc.clear();
            sc.DcpNames.addAll(dcpNames);
            sc.ExplicitDcpAddrs.addAll(dcpAddresses.stream().map(DcpAddress::new).toList());
            sc.setAscendingTimeOnly(ascending);
            sc.single = single;
            sc.setLrgsUntil(until);
            sc.setLrgsSince(since);
            if (spaceCraft != null)
            {
                sc.spacecraft = spaceCraft.toChar();
            }
            mar.setSearchCriteria(sc);
            mar.init();
            sources.forEach(
                s -> lrgs.getInputs()
                         .stream()
                         .filter(input -> input.getInputName().equals(s))
                         .map(i -> i.getSlot())
                         .forEach(sc::addSource)
            );

            var result = MessageRetrieval.getMessages(mar, lrgs, Integer.MAX_VALUE);
            if (result.ex() != null)
            {
                // Just do it again. This is bit rediculous but for some reason the message archive retriever
                // won't return any data on the initial request. Appears to be something to do with timing; however,
                // this initial change is not the place to fix it.
                result = MessageRetrieval.getMessages(mar, lrgs, Integer.MAX_VALUE);
            }
            return switch (result.ex())
            {
                case UntilReachedException ur -> handleArchiveError.apply(result.messages(), result.ex());
                case SearchTimeoutException st -> handleArchiveError.apply(result.messages(), result.ex());
                case EndOfArchiveException ea ->  handleArchiveError.apply(result.messages(), result.ex());
                case null -> Response.ok().entity(result.messages()).build();
                default -> Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                            .entity(MESSAGE_RETRIEVE_FAILED)
                            .build();
            };
            //return Response.ok().entity(result.messages()).build();
        }
        catch (IOException | SearchSyntaxException | ArchiveUnavailableException ex)
        {
            log.error("can't get messages.", ex);
            return Response.status(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
                        .entity(MESSAGE_RETRIEVE_FAILED)
                        .build();
        }
    }

    @GET
    @Path("/sources")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSources(@Context HttpServletRequest request)
    {
        LrgsMain lrgs = (LrgsMain)servletContext.getAttribute("lrgs");
        var sources = lrgs.getInputs()
                          .stream()
                          .filter(i -> i != null)
                          .filter(i -> i.getStatusCode() != LrgsInputInterface.DL_DISABLED)
                          .map(i -> i.getInputName())
                          .toList();
        return Response.ok().entity(sources).build();
    }

    public static class Message
    {
        public List<String> messages;
    }
}
