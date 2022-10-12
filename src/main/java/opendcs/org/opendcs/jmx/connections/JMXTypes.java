package opendcs.org.opendcs.jmx.connections;

import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public final class JMXTypes
{
    public static TabularType CONNECTION_LIST;
    public static CompositeType CONNECTION_LIST_TYPE;

    static
    {
        try
        {
            createConnectionTypes();
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    private static void createConnectionTypes() throws Exception
    {
        String[] itemNames = new String[1];
        itemNames[0] = "Lifetime";
        OpenType<?>[] itemTypes = new OpenType[1];
        itemTypes[0] = SimpleType.LONG;
        String[] itemDescription = new String[1];
        itemDescription[0] = "Lifetime in Seconds";
        
        CONNECTION_LIST_TYPE = new CompositeType("ConnectionListRow", "Specific information about a connection", itemNames,itemDescription,itemTypes);

        CONNECTION_LIST = new TabularType("ConnectionList", "List of connections and current state", CONNECTION_LIST_TYPE, itemNames);
        
    }
}
