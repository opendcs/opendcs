package org.opendcs.regression_tests.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.decoder.DecodedMessage;

public class SingletonMemoryConsumer extends DataConsumer
 {
    private final ConcurrentHashMap<String, List<DecodedMessage>> messages = new ConcurrentHashMap<>();

    private static final SingletonMemoryConsumer INSTANCE = new SingletonMemoryConsumer();    

    @Override
    public void open(String consumerArg, Properties props) throws DataConsumerException
    {
    
    }

    @Override
    public void close() 
    {
    
    }

    @Override
    public void startMessage(DecodedMessage msg) throws DataConsumerException
    {
        var address = msg.getRawMessage().getMediumId();
        INSTANCE.messages.computeIfAbsent(address, addr -> Collections.synchronizedList(new ArrayList<>()))
                .add(msg);
    }

    @Override
    public void println(String line)
    {
    
    }

    @Override
    public void endMessage()
    {
    
    }

    public static List<DecodedMessage> messagesFor(String address)
    {
        var list = INSTANCE.messages.get(address);
        return Collections.unmodifiableList(list != null ? list : List.of());
    }

    
}
