/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package org.opendcs.lrgs.webhook.dadds;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.opendcs.lrgs.dao.MsgArchive;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Path("/webhook/dadds")
public class DaddsWebHookResource
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final ObjectMapper jsonMapper = JsonMapper.builder()
                                                             .enable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES)
                                                             .addModule(new JavaTimeModule())
                                                             .build();

    private final HashMap<Region, SnsMessageManager> snsManagers = new HashMap<>();
    
    @Context
    ServletContext servletContext;

    // Dadds WebHooks are always SNS messages
    @POST
    @Path("{hookId}")
    public Response handleHook(@PathParam("hookId") String hookId,
                               @HeaderParam("x-amz-sns-message-type") String msgType,
                               @HeaderParam("x-amz-sns-topic-arn") String topicArn,
                               String message)
    {
        // validate hook id
        // validate subscription (if that's the message)
        // validate signature
        // process message
        
        var hook = valiateHookId(hookId);

        if (hook == null)
        {
            log.warn("Attempt to post to hookId that doesn't exist.");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        try
        {
            var region = parseRegion(topicArn);
            if (region == null)
            {
                throw SdkClientException.create("Region could not be parsed from message topicArn");
            }
            var snsMessage = snsManagers.computeIfAbsent(region, r -> SnsMessageManager.builder()
                                                                                       .region(region)
                                                                                       .build())
                                        .parseMessage(message);
            return switch (snsMessage.type())
            {
                case SUBSCRIPTION_CONFIRMATION -> confirmSubscription((SnsSubscriptionConfirmation)snsMessage);
                case NOTIFICATION -> processMessage(snsMessage, hook);
                default -> Response.status(Response.Status.NOT_FOUND).build();
            };
        }
        catch (SdkClientException ex)
        {
            log.atError().setCause(ex).log("Invalid message sent");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Region parseRegion(String topicArn)
    {
        log.trace("Topic Arn {}", topicArn);
        String[] parts = topicArn != null ? topicArn.split(":") : new String[0];
        if (parts.length < 4)
        {
            return null;
        }
        return Region.of(parts[3]);
    }

    private Response confirmSubscription(SnsSubscriptionConfirmation snsMessage)
    {
        try(var snsClient = SnsClient.create())
        {
            snsClient.confirmSubscription(b -> b.token(snsMessage.message()).topicArn(snsMessage.topicArn()));
            return Response.ok().build();
        }
        catch (Exception ex) // NOSONAR
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
    }

    private Response processMessage(SnsMessage snsMessage, DaddsWebHookInput hookInput)
    {
        try
        {
            log.trace("Received: {}", snsMessage.message());
            var message = jsonMapper.readValue(snsMessage.message(), DaddsDataMessage.class);
            var archive = (MsgArchive)servletContext.getAttribute("archive");
            var dcpMessage = new DcpMsg();
            dcpMessage.setDcpAddress(new DcpAddress(message.address()));
            dcpMessage.setData(message.data().getBytes(StandardCharsets.US_ASCII));
            archive.archiveMsg(dcpMessage, hookInput);
            return Response.ok().build();
        }
        catch (JsonProcessingException ex)
        {
            log.atWarn().setCause(ex).log("Unable to process data message.");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @SuppressWarnings("unchecked")
    private DaddsWebHookInput valiateHookId(String hookId)
    {
        var hooks = (Map<String,DaddsWebHookInput>)servletContext.getAttribute("hooks");
        return hooks.get(hookId);
    }
}
