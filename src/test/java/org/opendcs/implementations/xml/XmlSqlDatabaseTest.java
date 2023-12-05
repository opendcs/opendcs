package org.opendcs.implementations.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
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
    public void test_sql_update() throws Exception
    {
        XmlSqlDatabaseIO dbio = (XmlSqlDatabaseIO)db.getDbIo();
        try (Connection c = dbio.getConnection();
             Statement stmt = c.createStatement())
        {
            stmt.executeUpdate("insert into enum values(1,'test')");

            stmt.executeUpdate("update enum set name='test_update' where id=1");
        }
    }

    @Test
    @Disabled
    public void test_enum_read_write() throws Exception
    {
        final EnumList list = db.enumList;
        assertTrue(list.getEnumList().isEmpty(), "Unknown elements have been added to what should be an empty list of enums");
        final DbEnum insertedEnum = new DbEnum(DbKey.NullKey,"test");
        insertedEnum.addValue("property1", "first enum", null, null);
        insertedEnum.addValue("property2","second enum",null,null);
        list.addEnum(insertedEnum);
        list.write();
        assertTrue(list.size() > 0, "Enum was not saved to the database.");

        final DbEnum retrievedEnum = list.getEnum("test");
        assertNotNull(retrievedEnum);
        assertTrue(retrievedEnum.size() > 0, "Enum Values were not saved to the enum.");

        retrievedEnum.addValue("property3", "3rd enum", XmlSqlDatabaseTest.class.getName(), null);
        list.write();

        final DbEnum thirdEnum = list.getEnum("test");
        final EnumValue value = thirdEnum.findEnumValue("property3");
        assertEquals("3rd enum",value.getDescription());
        assertEquals(XmlSqlDatabaseTest.class.getName(),value.getExecClassName());

        list.remove(thirdEnum);
        list.write();
        assertTrue(list.size() == 0, "Failed to delete enum.");
        final DbEnum failedFind = list.getEnum("test");
        assertNull(failedFind, "Enum exists even though list is empty.");
    }
}
