package decodes.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.opendcs.utils.FailableResult;

import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;

public class DataSourceTest
{

    @Test
    void test_datasource_stream() throws Exception
    {
        TestDataSource testDS = new TestDataSource();
        List<FailableResult<RawMessage,DataSourceException>> results = testDS.getRawMessages().collect(Collectors.toList());
        assertEquals(testDS.size(), results.size(), "All messages and the end marker have not been returned.");
        assertArrayEquals("A message".getBytes(),results.get(0).getSuccess().data, "Correct data was not returned.");
    }


    @Test
    void test_datasource_stream_collector() throws Exception
    {
        TestDataSource testDS = new TestDataSource();
        DataSourceException result = testDS.getRawMessages()
                                           .filter(msg -> msg.isSuccess())
                                           .map(msg -> msg.getSuccess())
                                           .map(msg -> {
                                                System.out.println("Decoded Message.");
                                                return msg;
                                           })
                                           .collect(new MessageCollector());
        assertNotNull(result);
        
    }


    public static class TestDataSource extends DataSourceExec
    {
        private int index = 0;
        private String[] messages = {"A message", "B message", "C Message"};        

        public TestDataSource()
        {
            super(null, null);
        }

        @Override
        public void processDataSource() throws InvalidDatabaseException 
        {
            /* nothing to do */
        }

        public int size()
        {
            return messages.length;
        }

        @Override
        public void init(Properties routingSpecProps, String since, String until, Vector<NetworkList> networkLists) throws DataSourceException 
        {
            /* nothing to do */
        }

        @Override
        public void close()
        {
            /* nothing to do */
        }

        @Override
        public RawMessage getRawMessage() throws DataSourceException
        {
            if (index >= messages.length)
            {
                throw new DataSourceEndException("No more data.");
            }
            if (index > 0)
            {
                /* outputing message with some delay to see the affects */
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            RawMessage msg = new RawMessage(messages[index].getBytes());
            index++;
            return msg;
        }
    }

    public static class MessageCollector implements Collector<RawMessage, List<RawMessage>, DataSourceException>
    {

        @Override
        public Supplier<List<RawMessage>> supplier()
        {
            /*
             This type is arbitrary but the examples used Lists. I'm thinking the instance of the collector
             takes a "StatusReporter" object as a constructor parameter and this is what's returned.
            */
            return () -> new ArrayList<RawMessage>();
        }

        @Override
        public BiConsumer<List<RawMessage>, RawMessage> accumulator()
        {
            return (list,msg) ->
            {
                /** Here instead of "Accumulating" we'd be updating a status object */
                System.out.println(new Date().toString() + ": Adding message: " + msg.toString());
                list.add(msg);
            };
        }

        /**
         * This only comes up when doing parallel, but for our purpose the same logic as above would seem to
         * be able to apply
         */
        @Override
        public BinaryOperator<List<RawMessage>> combiner() {
            return (left,right) ->
            {
                System.out.println("Combining");
                left.addAll(right);
                return left;
            };
        }

        @Override
        public Function<List<RawMessage>, DataSourceException> finisher() 
        {            
            return (list) -> 
            {
                for (RawMessage rm :list)
                {
                    System.out.println(new Date().toString() + ": " + rm);
                }
                System.out.println(new Date().toString() + ": Finishing.");
                return new DataSourceEndException("The end");
            };    
        }

        @Override
        public Set<Characteristics> characteristics() 
        {
            Set<Characteristics> set = new HashSet<>();
            set.add(Characteristics.UNORDERED);
            return set;
        }

    }
}
