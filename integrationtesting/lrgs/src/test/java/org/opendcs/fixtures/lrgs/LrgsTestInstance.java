package org.opendcs.fixtures.lrgs;

import static org.opendcs.fixtures.assertions.Waiting.assertResultWithinTimeFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opendcs.tls.TlsMode;

import ilex.util.QueueLogger;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

import uk.org.webcompere.systemstubs.security.SystemExit;

public class LrgsTestInstance
{
    private final LrgsMain lrgs;
    private final MsgArchive archive;
    private final QueueLogger queueLogger;
    private final File configFile;
    private final Thread lrgsThread;

    public LrgsTestInstance(File lrgsHome) throws Exception
    {
        this(lrgsHome, null, null, TlsMode.NONE);
    }

    public LrgsTestInstance(File lrgsHome, File keyStore, String keyStorePassword, TlsMode tlsMode) throws Exception
    {
        if (!lrgsHome.canRead())
        {
            throw new FileNotFoundException(
                String.format("Directory '%s' doesn't exist or can't be read.", lrgsHome.getAbsolutePath())
            );
        }
        System.setProperty("LRGSHOME", lrgsHome.getAbsolutePath());
        configFile = new File(lrgsHome,"lrgsconf");
        try (FileWriter fw = new FileWriter(configFile))
        {
            fw.write("archiveDir=$LRGSHOME/archive"+System.lineSeparator());
            fw.write("hritTimeoutSec=120"+System.lineSeparator());
            fw.write("hritInputDir=$LRGSHOME/hritfiles"+System.lineSeparator());
            fw.write("hritFileMaxAgeSec=7200"+System.lineSeparator());
            fw.write("hritSourceCode=HR"+System.lineSeparator());
            fw.write("hritFileEnabled=true"+System.lineSeparator());
            fw.write("noTimeout=true"+System.lineSeparator());
            fw.write("ddsListenPort=0"+System.lineSeparator());
            fw.write("enableDdsRecv=true"+System.lineSeparator());
            fw.write("ddsServerTlsMode="+tlsMode.name()+System.lineSeparator());

            if (keyStore!=null) {
                String fileName =keyStore.getAbsolutePath();
                fileName = fileName.replace('\\','/');
                fw.write("keyStoreFile="+fileName+System.lineSeparator());
                fw.write("keyStorePassword="+keyStorePassword+System.lineSeparator());
            }
            fw.flush();
        }
        new File(lrgsHome,"netlist").mkdirs();
        queueLogger = new QueueLogger("");
        SystemExit exit = new SystemExit();
        lrgs = new LrgsMain("-", configFile.getAbsolutePath());

        lrgsThread = new Thread(lrgs);
        exit.execute(() -> lrgsThread.start());
        assertResultWithinTimeFrame(value ->
        {
            try
            {
                return lrgs.getStatusProvider().getStatusSnapshot().isUsable;
            }
            catch (NullPointerException ex)
            {
                // Future work should remove the need for this NPE catch.
                return false;
            }
        }, 3, TimeUnit.MINUTES, 5, TimeUnit.SECONDS,
        "LRGS has not started within the expected time frame.");

        this.archive = lrgs.msgArchive;
    }

    public MsgArchive getArchive()
    {
        return archive;
    }

    public int getDdsPort()
    {
        return lrgs.getDdsServer().getPort();
    }

    public LrgsConfig getConfig()
    {
        return LrgsConfig.instance();
    }


    public List<LrgsInputInterface> getLrgsInputs()
    {
        return lrgs.getInputs();
    }
}
