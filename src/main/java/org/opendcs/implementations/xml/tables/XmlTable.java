package org.opendcs.implementations.xml.tables;

import java.lang.reflect.Type;
import java.util.List;

import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptTable.ToRelContext;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.QueryableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class XmlTable extends AbstractTable implements QueryableTable, TranslatableTable
{
    @Override
    public boolean isRolledUp(String column)
    {
        return false;
    }

    @Override
    public boolean rolledUpColumnValidInsideAgg(String column, SqlCall call, @Nullable SqlNode parent,
            @Nullable CalciteConnectionConfig config)
    {
        return false;
    }

    @Override
    public <T> Queryable<T> asQueryable(QueryProvider queryProvider, SchemaPlus schema, String tableName)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'asQueryable'");
    }

    @Override
    public Type getElementType()
    {
        return Object[].class;
    }

    public abstract List<RelDataType> getFieldTypes();


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
}
