package decodes.xml;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.DecodesScript;
import decodes.db.EmptyDecodesScriptReader;
import decodes.db.PlatformConfig;
import decodes.util.DecodesSettings;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class XmlDatabaseTest {

    /**
     * This test checks reading and writing decodes scripts to a new empty xml decodes database
     * @throws Exception
     */
    @Test
    public void createTwoDecodesScriptsThenRead() throws Exception
    {
        Path xmlDir = Files.createTempDirectory("xml-test-dir-{101}");
        System.out.println(xmlDir.toString());
        String configName = "config-1";
        XmlDatabaseIO dbio= (XmlDatabaseIO) DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XML,xmlDir.toString());

        Database db = new Database();
        db.setDbIo(dbio);

        PlatformConfig pc = new PlatformConfig(configName);
        EmptyDecodesScriptReader e = new EmptyDecodesScriptReader();

        DecodesScript ds = DecodesScript.from(e).platformConfig(pc).build();
        ds.scriptName = "Script1";
        pc.decodesScripts.add(ds);
        db.platformConfigList.add(pc);

        ds = DecodesScript.from(e).platformConfig(pc).build();
        ds.scriptName = "Script2";
        pc.decodesScripts.add(ds);
        db.platformConfigList.add(pc);

        if( 2 != pc.getNumScripts())
            throw new Exception("Expected 2 scripts (before save).  Found "+pc.getNumScripts());

        db.write();

        // read from disk
        db = new Database();
        db.setDbIo(dbio);
        db.read();

        PlatformConfig cfg = db.platformConfigList.get(configName);

        if( 2 != cfg.getNumScripts())
            throw new Exception("Expected 2 scripts (after reading from disk).  Found "+cfg.getNumScripts());
        assertEquals(2, cfg.getNumScripts());

        // TODO delete temporary directory?
    }

}