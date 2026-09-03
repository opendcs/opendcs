package org.opendcs.fixtures.inet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;

/**
 * Intercept various DNS requests to make testing protocols, such as the Amazon SNS webhook flows, easier to deal with.
 * InterceptingInetAddressResolver
 */
@ServiceProvider(service = InetAddressResolverProvider.class)
public class InterceptingInetAddressResolver extends InetAddressResolverProvider
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final HashMap<String, byte[]> addresses = new HashMap<>();
   
    @Override
    public InetAddressResolver get(Configuration configuration)
    {
        return new InetAddressResolver()
        {
            private InetAddressResolver defaultResolver = configuration.builtinResolver();

            @Override
            public String lookupByAddress(byte[] addr) throws UnknownHostException
            {
                String ret = addresses.entrySet().stream()
                                           .filter(es -> Arrays.equals(es.getValue(),addr))
                                           .filter(Objects::nonNull)
                                           .findFirst()
                                           .map(Entry::getKey)
                                           .orElse(null);
                if (ret == null)
                {
                    ret = defaultResolver.lookupByAddress(addr);
                }

                return ret;
            }

            @Override
            public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy)
                    throws UnknownHostException 
            {
                var addrBytes = addresses.get(host);
                if (addrBytes == null)
                {
                    log.trace("host '{}' was not found in intercept registry, passing on to default lookup", host);
                    return defaultResolver.lookupByName(host, lookupPolicy);
                }
                log.trace("For Host '{}' returning override address", host);
                return Stream.of(InetAddress.getByAddress(addrBytes));
            }
        };
    }

    public static void registerIntercept(String host, InetAddress addr)
    {
        addresses.put(host, addr.getAddress());
    }

    public static void removeIntercept(String host)
    {
        addresses.remove(host);
    }

    @Override
    public String name()
    {
        return InterceptingInetAddressResolver.class.getSimpleName();
    }
    
}
