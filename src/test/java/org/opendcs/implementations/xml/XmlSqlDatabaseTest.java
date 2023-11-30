package org.opendcs.implementations.xml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

@TestInstance(Lifecycle.PER_CLASS)
public class XmlSqlDatabaseTest
{
    private Database db;

    @BeforeAll
    public void setup() throws Exception
    {
        Path xmlDir = Files.createTempDirectory("xml-sql-test-dir");
        System.out.println(xmlDir.toString());
        XmlSqlDatabaseIO dbio = (XmlSqlDatabaseIO) DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XMLCALCITE, xmlDir.toString());

        db = new Database(true);
        db.setDbIo(dbio);
        // TODO: eventually change to db.read() once all are implemented
        dbio.readEnumList(db.enumList);
    }

    @Test
    public void test_enum_read_write() throws Exception
    {
        EnumList list = db.enumList;
        assertTrue(list.getEnumList().isEmpty(), "Unknown elements have been added to what should be an empty list of enums");
        DbEnum e = new DbEnum(DbKey.NullKey,"test");
        e.addValue("property1", "first enum", null, null);
        e.addValue("property2","second enum",null,null);
        list.addEnum(e);
        list.write();
    }
}
