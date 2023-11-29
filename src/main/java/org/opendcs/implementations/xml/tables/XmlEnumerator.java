package org.opendcs.implementations.xml.tables;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.util.Source;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.db.DatabaseObject;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.tsdb.NoSuchObjectException;
import decodes.xml.TopLevelParser;

import static org.apache.calcite.linq4j.Nullness.castNonNull;

public class XmlEnumerator implements Enumerator<Object[]>
{
    private final AtomicBoolean cancelFlag;
    private final List<RelDataType> fieldTypes;
    private final List<Integer> fields;
    private final EnumList list;
    private Iterator<DbEnum> iterator;
    private Object[] current;

    XmlEnumerator(EnumList enums, AtomicBoolean cancelFlag,
                  List<RelDataType> fieldTypes, List<Integer> fields)
    {
        this.list = enums;
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
        return current;
    }

    public Object[] convert(DbEnum theEnum)
    {
        Object []fields = new Object[2];
        fields[0] = theEnum.getId().getValue();
        fields[1] = theEnum.getUniqueName();
        return fields;
    }

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
            current = convert(iterator.next());
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
