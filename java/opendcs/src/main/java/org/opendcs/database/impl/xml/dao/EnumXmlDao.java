package org.opendcs.database.impl.xml.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesSettings;
import decodes.xml.jdbc.XmlConnection;
import opendcs.dai.EnumDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DbObjectCache;
import opendcs.util.functional.DaoConsumer;

public class EnumXmlDao implements EnumDAI
{
    private static final Logger log = LoggerFactory.getLogger(EnumXmlDao.class);
    private final DecodesSettings settings;
    private final DbObjectCache<DbEnum> cache;

    public EnumXmlDao(DecodesSettings settings, DbObjectCache<DbEnum> cache)
    {
        this.settings = settings;
        this.cache = cache;
    }


    @Override
    public ResultSet doQuery(String q) throws DbIoException
    {
        throw new UnsupportedOperationException("Unimplemented method 'doQuery'");
    }

    @Override
    public ResultSet doQuery2(String q) throws DbIoException
    {
        throw new UnsupportedOperationException("Unimplemented method 'doQuery2'");
    }

    @Override
    public int doModify(String q) throws DbIoException
    {
        throw new UnsupportedOperationException("Unimplemented method 'doModify'");
    }

    @Override
    public void setManualConnection(Connection conn)
    {
        throw new UnsupportedOperationException("Unimplemented method 'setManualConnection'");
    }

    @Override
    public void inTransactionOf(DaoBase other) throws IllegalStateException 
    {
        throw new UnsupportedOperationException("Unimplemented method 'inTransactionOf'");
    }

    @Override
    public void inTransaction(DaoConsumer consumer) throws Exception
    {
        throw new UnsupportedOperationException("Unimplemented method 'inTransaction'");
    }

    @Override
    public DbEnum getEnum(String enumName) throws DbIoException
    {

        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEnum'");
    }

    @Override
    public void readEnumList(EnumList top) throws DbIoException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readEnumList'");
    }

    @Override
    public void writeEnumList(EnumList enumList) throws DbIoException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeEnumList'");
    }

    @Override
    public void writeEnum(DbEnum dbenum) throws DbIoException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeEnum'");
    }

    @Override
    public void close()
    {
        /** no-op in new scheme */
    }

    @Override
    public Collection<DbEnum> getEnums(DataTransaction tx) throws OpenDcsDataException
    {
        XmlConnection conn = tx.connection(XmlConnection.class).get();

        Collection<DbEnum> enumList = new HashSet<>();
        XMLInputFactory f = XMLInputFactory.newInstance();
        File xmlFile = new File(conn.getDirectory(), "enum/EnumList.xml");
        
        try (InputStream stream = new FileInputStream(xmlFile))
        {
            XMLEventReader reader = f.createXMLEventReader(stream);
            while(reader.hasNext())
            {
                DbEnum curEnum = null;
                EnumValue curValue = null;
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement())
                {
                    StartElement se = event.asStartElement();
                    String elementName = se.getName().getLocalPart();
                    if (elementName.equals("Enum"))
                    {    
                        curEnum = new DbEnum(se.getAttributeByName(QName.valueOf("Name")).getValue());
                    }
                    else if (elementName.equals("EnumValue"))
                    {
                        curValue = new EnumValue(curEnum, se.getAttributeByName(QName.valueOf("EnumValue")).getValue());
                    }
                    else if (elementName.equals("Description"))
                    {
                        event = reader.nextEvent();
                        if (curValue != null)
                        {
                            curValue.setDescription(event.asCharacters().getData());
                        }
                    }
                }
                else if (event.isEndElement())
                {
                    if (event.asEndElement().getName().getLocalPart().equals("Enum"))
                    {
                        enumList.add(curEnum);
                        cache.put(curEnum);
                        curEnum = null;
                    }
                    else if (event.asEndElement().getName().getLocalPart().equals("EnumValue"))
                    {
                        curEnum.addValue(curValue);
                        curValue = null;
                    }
                }
            }
        
            return enumList;
        }
        catch (IOException | XMLStreamException ex)
        {
            throw new OpenDcsDataException("Unable to read enum list.", ex);
        }
    }   

    @Override
    public Optional<DbEnum> getEnum(DataTransaction tx, String enumName) throws OpenDcsDataException 
    {
        DbEnum ret = null;
        ret = cache.getByUniqueName(enumName);
        if (ret == null)
        {
            Collection<DbEnum> enums = getEnums(tx);
            for (DbEnum dbEnum: enums)
            {
                if (dbEnum.getUniqueName().equals(enumName))
                {
                    ret = null;
                    break;
                }
            }
        }
        return Optional.ofNullable(ret);
    }

    @Override
    public Optional<DbEnum> getEnum(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return Optional.empty();
    }

    @Override
    public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException 
    {
        XmlConnection conn = (XmlConnection)tx.connection(Connection.class).get();
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        /**
         * NOTE: at this time we're intentionally not worrying about the need to read this list back first.
         */
        File xmlFile = new File(conn.getDirectory(), "enum/EnumList.xml");
        try (OutputStream outStream = new FileOutputStream(xmlFile))
        {
            XMLStreamWriter writer = f.createXMLStreamWriter(outStream);
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("EnumList");
                writer.writeStartElement("Enum");
                    writer.writeAttribute("Name", dbEnum.enumName);
                    writer.writeStartElement("Description");
                        writer.writeCharacters(dbEnum.getDescription());
                    writer.writeEndElement();
                    Iterator<EnumValue> it = dbEnum.iterator();
                    while(it.hasNext())
                    {
                        EnumValue curVal = it.next();
                        writer.writeStartElement("EnumValue");
                            writer.writeAttribute("EnumValue", curVal.getValue());
                            writer.writeStartElement("Description");
                            writer.writeCharacters(curVal.getDescription());
                            writer.writeEndElement();
                            writer.writeStartElement("SortNumber");
                                writer.writeCharacters(""+curVal.getSortNumber());
                            writer.writeEndElement();
                        
                        writer.writeEndElement();
                    }
                writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
        }
        catch (IOException | XMLStreamException ex)
        {
            throw new OpenDcsDataException("Unable to write enum to XML database.",ex);
        }
        return dbEnum;
    }
    
}
