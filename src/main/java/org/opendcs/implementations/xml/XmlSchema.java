package org.opendcs.implementations.xml;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import org.xml.sax.SAXException;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.xml.XmlDatabaseIO;

public class XmlSchema extends AbstractSchema
{
    private final Database db;
    private final XmlDatabaseIO xmlDb;
    private final Schema parent;
    private final Map<String, Table> tableMap = new HashMap<>();

    public XmlSchema(Schema parent, String location)
    {
        this.parent = parent;
        try
        {
            db = new Database(true);
            xmlDb = new XmlDatabaseIO(location);
            db.setDbIo(xmlDb);
            db.read();
        }
        catch (ParserConfigurationException| SAXException | DatabaseException ex)
        {
            throw new RuntimeException("Unable to open XML database.",ex);
        }
    }

    @Override
    public Map<String, Table> getTableMap()
    {
        return tableMap;
    }

    public XmlDatabaseIO getDbIo()
    {
        return xmlDb;
    }

    public Database getDb()
    {
        return db;
    }
}
