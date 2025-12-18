package decodes.datasource;

import java.util.Properties;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import decodes.db.NetworkList;

class WebSourceTest
{
    @Test
    void test_abstract_web_source() throws Exception
    {
        Properties props = new Properties();
        Vector<NetworkList> netlists = new Vector<>();

        WebAbstractDataSource wads = new WebAbstractDataSource(null, null);

        props.setProperty("abstractUrl", "http://localhost:80/${MEDIUMID}/${SINCE}/${UNTIL}");
        wads.init(props, "now - 2 hours", "now", netlists);
    }

}
