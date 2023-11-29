package org.opendcs.implementations.xml.tables;

import java.util.Iterator;
import java.util.ArrayList;

import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.db.HasIterator;

public class EnumValuesList implements HasIterator<EnumValue>
{
    ArrayList<EnumValue> items = new ArrayList<>();
    protected EnumList list;

    public EnumValuesList(EnumList list)
    {
        this.list = list;     
        this.list.getEnumList().forEach(e -> 
        {
            items.addAll(e.values());
        });
    }

    @Override
    public Iterator<EnumValue> iterator()
    {
        return items.iterator();
    }    
    
}
