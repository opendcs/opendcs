package org.opendcs.implementations.xml.tables;

import java.util.List;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.config.CalciteConnectionConfig;
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
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.ImmutableIntList;
import org.apache.calcite.util.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.opendcs.implementations.xml.XmlSchema;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.xml.XmlDatabaseIO;


public class XmlEnumTable extends XmlTable
{
    private final XmlSchema schema;
    private final String name;
    private final RelProtoDataType type;

    private List<RelDataType> fieldTypes;

    public XmlEnumTable(XmlSchema schema, String tableName, RelProtoDataType relProtoDataType) throws DatabaseException
    {
        this.schema = schema;
        this.name = tableName;
        this.type = relProtoDataType;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory)
    {
        
        List<String> names = new ArrayList<>();
        List<RelDataType> types = new ArrayList<>();
        names.add("id");
        names.add("name");
        types.add(typeFactory.createSqlType(SqlTypeName.BIGINT));
        types.add(typeFactory.createSqlType(SqlTypeName.VARCHAR));
        this.fieldTypes = types;
        return typeFactory.createStructType(Pair.zip(names,types));
    }    

    public List<RelDataType> getFieldTypes()
    {
        return fieldTypes;
    }

    @Override
    public Expression getExpression(SchemaPlus schema, String table, Class clazz)
    {
        return Schemas.tableExpression(schema, getElementType(), table, clazz);
    }

    @Override
    public RelNode toRel(ToRelContext context, RelOptTable relOptTable)
    {
        return new XmlTableScan(context.getCluster(),relOptTable,this);
    }
    
    /** Returns an enumerable over a given projection of the fields. */
  @SuppressWarnings("unused") // called from generated code
  public Enumerable<Object[]> project(final DataContext root,
      final int[] fields) {
        final AtomicBoolean cancelFlag = DataContext.Variable.CANCEL_FLAG.get(root);
        return new AbstractEnumerable<Object[]>() {
            @Override public Enumerator<Object[]> enumerator() {
                JavaTypeFactory typeFactory = root.getTypeFactory();
                    return new XmlEnumerator<EnumList,DbEnum>(schema.getDb().enumList, cancelFlag,
                        getFieldTypes(), ImmutableIntList.of(fields))
                        {
                            @Override
                            public Object[] convert(DbEnum theValue) {
                                {
                                    Object []fields = new Object[2];
                                    fields[0] = theValue.getId().getValue();
                                    fields[1] = theValue.getUniqueName();
                                    return fields;
                                }
                            }
                            
                        };
      }
    };
  }
}