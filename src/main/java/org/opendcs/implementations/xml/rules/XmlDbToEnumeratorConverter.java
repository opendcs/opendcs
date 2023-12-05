package org.opendcs.implementations.xml.rules;

import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;

import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterImpl;


public class XmlDbToEnumeratorConverter extends ConverterImpl implements XmlDbRel
{

    protected XmlDbToEnumeratorConverter(RelOptCluster cluster, RelTraitSet traits,
            RelNode child) 
    {
        super(cluster, ConventionTraitDef.INSTANCE, traits, child);
    }
    
}
