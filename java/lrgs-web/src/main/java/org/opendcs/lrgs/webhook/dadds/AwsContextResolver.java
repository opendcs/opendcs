package org.opendcs.lrgs.webhook.dadds;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.regions.Region;

@Provider
@Singleton
public class AwsContextResolver implements ContextResolver<AwsContextResolver.AwsContext>
{
    private final AwsContext context = new AwsContext();

    public record AwsContext(ConcurrentHashMap<Region, SnsMessageManager> snsMessageManagers)
    {
        public AwsContext()
        {
            this(new ConcurrentHashMap<>());
        }
    }

    @Override
    public AwsContext getContext(Class<?> type)
    {
        return context;
    }
}
