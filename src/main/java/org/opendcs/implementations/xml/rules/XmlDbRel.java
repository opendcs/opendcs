package org.opendcs.implementations.xml.rules;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;

public interface XmlDbRel extends RelNode
{
    Convention XML_CONVENTION = new Convention.Impl("XmlDb", XmlDbRel.class);

}
