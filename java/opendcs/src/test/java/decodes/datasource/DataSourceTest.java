package decodes.datasource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
            return () -> new ArrayList<RawMessage>();
        }

        @Override
        public BiConsumer<List<RawMessage>, RawMessage> accumulator()
        {
            return (list,msg) ->
            {
                System.out.println("Adding message");
                list.add(msg);
            };
        }

        @Override
        public BinaryOperator<List<RawMessage>> combiner() {
            return (left,right) -> {left.addAll(right);return left;};
        }

        @Override
        public Function<List<RawMessage>, DataSourceException> finisher() 
        {            
            return (list) -> 
            {
                System.out.println("Finishing.");
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
