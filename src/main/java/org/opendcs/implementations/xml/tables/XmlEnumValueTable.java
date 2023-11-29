package org.opendcs.implementations.xml.tables;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptTable.ToRelContext;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.ImmutableIntList;
import org.apache.calcite.util.Pair;
import org.opendcs.implementations.xml.XmlSchema;

import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.db.EnumList;
import decodes.db.HasIterator;

public class XmlEnumValueTable extends XmlTable
{
    private final XmlSchema schema;
    private final String name;

    private List<RelDataType> fieldTypes;

     public XmlEnumValueTable(XmlSchema schema, String tableName) throws DatabaseException
    {
        this.schema = schema;
        this.name = tableName;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory)
    {/*  enumId, enumValue, description, execClass, editClass */
         List<String> names = new ArrayList<>();
        List<RelDataType> types = new ArrayList<>();
        names.add("enumId");
        names.add("enumValue");
        names.add("description");
        names.add("execClass");
        names.add("editClass");
        types.add(typeFactory.createSqlType(SqlTypeName.BIGINT));
        types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        this.fieldTypes = types;
        return typeFactory.createStructType(Pair.zip(names,types));
    }

    public List<RelDataType> getFieldTypes()
    {
        return fieldTypes;
    }
    
    /** Returns an enumerable over a given projection of the fields. */
    @SuppressWarnings("unused") // called from generated code
      public Enumerable<Object[]> project(final DataContext root, final int[] fields)
      {
        final AtomicBoolean cancelFlag = DataContext.Variable.CANCEL_FLAG.get(root);
        return new AbstractEnumerable<Object[]>()
        {
            @Override public Enumerator<Object[]> enumerator()
            {
                JavaTypeFactory typeFactory = root.getTypeFactory();
                EnumValuesList list = new EnumValuesList(schema.getDb().enumList);
                return new XmlEnumerator<EnumValuesList,EnumValue>(list, cancelFlag, getFieldTypes(), ImmutableIntList.of(fields))
                    {/*  enumId, enumValue, description, execClass, editClass */
                        @Override
                        public Object[] convert(EnumValue theValue) {
                            {
                                Object []fields = new Object[5];
                                fields[0] = theValue.getDbenum().getId().getValue();
                                fields[1] = theValue.getValue();
                                fields[2] = theValue.getDescription();
                                fields[3] = theValue.getExecClassName();
                                fields[4] = theValue.getEditClassName();
                                return fields;
                            }
                        }
                    };
            }
        };
    }
}
