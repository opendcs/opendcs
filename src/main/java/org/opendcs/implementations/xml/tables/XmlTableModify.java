package org.opendcs.implementations.xml.tables;

import java.util.List;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.prepare.Prepare.CatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableModify;
import org.apache.calcite.rex.RexNode;
import org.checkerframework.checker.nullness.qual.Nullable;

public class XmlTableModify extends TableModify
{

    protected XmlTableModify(RelOptCluster cluster, RelTraitSet traitSet, RelOptTable table,
            CatalogReader catalogReader, RelNode input, Operation operation, @Nullable List<String> updateColumnList,
            @Nullable List<RexNode> sourceExpressionList, boolean flattened)
    {
        super(cluster, traitSet, table, catalogReader, input, operation, updateColumnList, sourceExpressionList, flattened);    
    }

    @Override
    public RelNode copy(RelTraitSet traitSet, List<RelNode> inputs)
    {
        return new XmlTableModify(this.getCluster(),
                                  traitSet,this.getTable(),
                                  this.getCatalogReader(),
                                  this.getInput(),
                                  this.getOperation(),
                                  this.getUpdateColumnList(),
                                  this.getSourceExpressionList(),
                                  this.isFlattened());
     }
    
}
