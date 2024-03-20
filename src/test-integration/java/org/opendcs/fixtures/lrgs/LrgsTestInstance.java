package org.opendcs.fixtures.lrgs;

import java.io.File;
import java.io.FileNotFoundException;

import ilex.util.FileLogger;
import ilex.util.QueueLogger;
import lrgs.archive.MsgArchive;
import lrgs.lrgsmain.LrgsMain;

public class LrgsTestInstance
{
    final private LrgsMain lrgs;
    final private MsgArchive archive;
    final private QueueLogger queueLogger;
    final private FileLogger fileLogger;
    final private File configFile;

    public LrgsTestInstance(File lrgsHome) throws FileNotFoundException
    {
        if (!lrgsHome.canRead())
        {
            throw new FileNotFoundException(
                String.format("Directory '%s' doesn't exist or can't be read.", lrgsHome.getAbsolutePath())
            );
        }
        configFile = new File(lrgsHome,"lrgsconf");
        queueLogger = new QueueLogger("");
        fileLogger = new FileLogger("lrgs", new File(lrgsHome,"lrgslog").getAbsolutePath(), 200*1024*1024);
        lrgs = new LrgsMain(queueLogger,"-", configFile.getAbsolutePath(), fileLogger);
        this.archive = lrgs.msgArchive;
    }
}
