package org.opendcs.fixtures.lrgs;

import static org.opendcs.fixtures.helpers.BackgroundTsDbApp.waitForResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import ilex.util.FileLogger;
import ilex.util.QueueLogger;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LrgsMain;

import uk.org.webcompere.systemstubs.security.SystemExit;

public class LrgsTestInstance
{
    private final LrgsMain lrgs;
    private final MsgArchive archive;
    private final QueueLogger queueLogger;
    private final FileLogger fileLogger;
    private final File configFile;
    private final Thread lrgsThread;

    public LrgsTestInstance(File lrgsHome) throws Exception
    {
        if (!lrgsHome.canRead())
        {
            throw new FileNotFoundException(
                String.format("Directory '%s' doesn't exist or can't be read.", lrgsHome.getAbsolutePath())
            );
        }
        System.setProperty("LRGSHOME", lrgsHome.getAbsolutePath());
        configFile = new File(lrgsHome,"lrgsconf");
        configFile.createNewFile();
        queueLogger = new QueueLogger("");
        fileLogger = new FileLogger("lrgs", new File(lrgsHome,"lrgslog").getAbsolutePath(), 200*1024*1024);
        SystemExit exit = new SystemExit();
        lrgs = new LrgsMain(queueLogger,"-", configFile.getAbsolutePath(), fileLogger);

        lrgsThread = new Thread(lrgs);
        exit.execute(() -> lrgsThread.start());
        waitForResult(value ->
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
        }, 1, TimeUnit.MINUTES, 15, TimeUnit.SECONDS);

        this.archive = lrgs.msgArchive;
    }

    public MsgArchive getArchive()
    {
        return archive;
    }
}
