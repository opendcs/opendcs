package org.opendcs.implementations.xml.tables;

import java.util.Map;
import java.net.URL;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.TableFactory;
import org.apache.calcite.util.Sources;
import org.checkerframework.checker.nullness.qual.Nullable;

import ilex.util.EnvExpander;

public class XmlTableFactory implements TableFactory<XmlTable>
{

    @Override
    public XmlTable create(SchemaPlus schema, String tableName, Map<String,Object> operand, @Nullable RelDataType rowType)
    {
        try
        {
            URL url = new URL(EnvExpander.expand("file:///$DCSTOOL_USERDIR/edit-db/enum/EnumList.xml"));
            XmlTable t = new XmlTable(schema,tableName, RelDataTypeImpl.proto(rowType),Sources.of(url));
            return t;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
