package org.opendcs.implementations.xml.util;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.adapter.enumerable.EnumerableRel;
import org.apache.calcite.adapter.enumerable.EnumerableRelImplementor;
import org.apache.calcite.adapter.enumerable.PhysType;
import org.apache.calcite.adapter.enumerable.PhysTypeImpl;
import org.apache.calcite.linq4j.tree.Blocks;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.core.TableScan;
import org.opendcs.implementations.xml.tables.XmlTable;

import com.google.common.collect.ImmutableList;

public class XmlTableScan extends TableScan implements EnumerableRel
{
    final XmlTable xmlTable;
    public XmlTableScan(RelOptCluster cluster, RelOptTable relOptTable, XmlTable xmlTable)
    {
        super(cluster, cluster.traitSetOf(EnumerableConvention.INSTANCE),ImmutableList.of(),relOptTable);
        this.xmlTable = xmlTable;
    }
    @Override
    public Result implement(EnumerableRelImplementor implementor, Prefer pref)
    {
        PhysType physType = PhysTypeImpl.of(implementor.getTypeFactory(), getRowType(), pref.preferArray());
        return implementor.result(
            physType,
            Blocks.toBlock(
                Expressions.call(table.getExpression(this.xmlTable.getClass()),
                                 "project",implementor.getRootExpression(),
                                 Expressions.constant(new int[xmlTable.getFieldTypes().size()])
                )
            )
        );
    }
}
