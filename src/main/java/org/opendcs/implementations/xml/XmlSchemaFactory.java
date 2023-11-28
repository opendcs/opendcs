package org.opendcs.implementations.xml;

import java.util.Map;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

import ilex.util.EnvExpander;

public class XmlSchemaFactory implements SchemaFactory
{
    public static final XmlSchemaFactory INSTANCE = new XmlSchemaFactory();

    @Override
    public Schema create(SchemaPlus parent, String name, Map<String, Object> operands)
    {
        return new XmlSchema(parent, EnvExpander.expand((String)operands.get("schema.dir")));
    }
}
