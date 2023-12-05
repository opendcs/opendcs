package org.opendcs.implementations.xml.rules;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.tools.RelBuilderFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Predicates;

public class XmlDbToEnumeratorRule extends ConverterRule
{
    public static final ConverterRule INSTANCE =
      new XmlDbToEnumeratorRule(RelFactories.LOGICAL_BUILDER);

    protected XmlDbToEnumeratorRule(Class<? extends RelNode> clazz, RelTrait in, RelTrait out,
            String descriptionPrefix)
    {
        super(clazz, in, out, descriptionPrefix);
    }

    public XmlDbToEnumeratorRule(RelBuilderFactory factory)
    {
        super(RelNode.class,Predicates.<RelNode>alwaysTrue(), XmlDbRel.XML_CONVENTION,EnumerableConvention.INSTANCE,factory,"XmlDbToEnumerableConverterRule");
    }

    @Override
    public @Nullable RelNode convert(RelNode rel) {
        RelTraitSet newTraitSet =rel.getTraitSet().replace(getOutConvention());
        return new XmlDbToEnumeratorConverter(rel.getCluster(),newTraitSet,rel);
    }
    
}
