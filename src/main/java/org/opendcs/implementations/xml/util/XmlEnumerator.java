package org.opendcs.implementations.xml.util;

import java.util.List;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;


import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;

import decodes.db.HasIterator;

public abstract class XmlEnumerator<L extends HasIterator<E>,E> implements Enumerator<Object[]>
{
    private final AtomicBoolean cancelFlag;
    private final List<RelDataType> fieldTypes;
    private final List<Integer> fields;
    private final L list;
    private Iterator<E> iterator;
    private E current;

    public XmlEnumerator(L list, AtomicBoolean cancelFlag,
                  List<RelDataType> fieldTypes, List<Integer> fields)
    {
        this.list = list;
        this.cancelFlag = cancelFlag;
        this.fieldTypes = fieldTypes;
        this.fields = fields;
        iterator = list.iterator();
    }
    @Override
    public void close()
    {
    }

    @Override
    public Object[] current()
    {
        return convert(current);
    }

    public abstract Object[] convert(E theValue);

    @Override
    public boolean moveNext()
    {
        // this loop matters more when filtering comes in to play
        if (this.cancelFlag.get())        
        {
            return false;
        }
        else if (iterator.hasNext())
        {
            current = iterator.next();
            return true;
        }
        else
        {
            current = null;
            return false;
        }        
    }

    @Override
    public void reset()
    {
        iterator = list.iterator();
    }
    
}
