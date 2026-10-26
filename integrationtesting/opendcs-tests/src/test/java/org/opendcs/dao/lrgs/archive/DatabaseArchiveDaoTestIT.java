package org.opendcs.dao.lrgs.archive;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.opendcs.data.lrgs.LrgsDataInterface;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.data.lrgs.archive.ArchiveEntry.Quality;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceDao;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceTypeDao;
import org.opendcs.database.dao.lrgs.archive.LrgsArchiveDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb("OpenDCS-Postgres")
class DatabaseArchiveDaoTestIT extends AppTestBase
{
    
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_basic_operations() throws Exception
    {
        var typeDao = db.getDao(LrgsDataInterfaceTypeDao.class).orElseThrow();
        var interfaceDao = db.getDao(LrgsDataInterfaceDao.class).orElseThrow();
        var archiveDao = db.getDao(LrgsArchiveDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            // setup interfaces
            var typeIn = new LrgsDataInterfaceType(null, "testType", "test", null, null);
            var typeOut = typeDao.save(tx, typeIn);

            var dataInterfaceIn = new LrgsDataInterface(null, "LrgsArchive", typeOut, Map.of("length_days", "1"), null, null);
            var dataInterfaceOut = interfaceDao.save(tx, dataInterfaceIn);

            var messageIn = new ArchiveEntry(dataInterfaceOut, "TEST_ID_1", null, 
                                             "Test Message".getBytes(StandardCharsets.UTF_8), null,
                                             null, Quality.GOOD);
            var messageOut = archiveDao.archive(tx, messageIn);
            assertTrue(messageOut.isSuccess());

        }
    }
}
