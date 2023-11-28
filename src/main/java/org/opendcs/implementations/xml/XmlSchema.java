package org.opendcs.implementations.xml;

import java.util.Collection;
import java.util.Set;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.xml.sax.SAXException;

import decodes.xml.XmlDatabaseIO;

public class XmlSchema extends AbstractSchema
{

    private XmlDatabaseIO xmlDb;
    private Schema parent;
    private Map<String, Table> tableMap;

    public XmlSchema(Schema parent, String location)
    {
        this.parent = parent;
        try
        {
            xmlDb = new XmlDatabaseIO(location);
        }
        catch (ParserConfigurationException| SAXException ex)
        {
            throw new RuntimeException("Unable to open XML database.",ex);
        }
    }

    @Override
    public Map<String, Table> getTableMap()
    {
        return tableMap;
    }
}
