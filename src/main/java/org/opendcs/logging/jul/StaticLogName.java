package org.opendcs.logging.jul;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

public class StaticLogName extends LogNamer
{

    public StaticLogName(String pattern)
    {
        super(pattern);
    }


   @Override
    public Path getFirstName(ZonedDateTime time) throws IOException
    {
        return Paths.get(pattern);
    }

    @Override
    public Path getNextName(Path currentLog, ZonedDateTime time) throws IOException
    {
        return currentLog;
    }

    @Override
    public boolean shouldRotate(Path log) throws IOException
    {
        return false;
    }
}
