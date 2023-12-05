package org.opendcs.implementations.xml;

import java.util.Map;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.opendcs.implementations.xml.tables.XmlEnumTable;
import org.opendcs.implementations.xml.tables.XmlEnumValueTable;

import decodes.db.DatabaseException;
import ilex.util.EnvExpander;

public class XmlSchemaFactory implements SchemaFactory
{
    public static final XmlSchemaFactory INSTANCE = new XmlSchemaFactory();

    @Override
    public Schema create(SchemaPlus parent, String name, Map<String, Object> operands)
    {
        XmlSchema schema = new XmlSchema(parent, EnvExpander.expand((String)operands.get("dir")));
        try 
        {
            schema.getTableMap().put("enum", new XmlEnumTable(schema,"enum",null));
            schema.getTableMap().put("enumvalue", new XmlEnumValueTable(schema, "enumvalue"));
        } catch (DatabaseException ex) {
            throw new RuntimeException("Unable to load database", ex);
        }
        return schema;
    }
}
