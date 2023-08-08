package org.opendcs.logging.jul;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;

public abstract class LogNamer
{
    protected String pattern;

    @SuppressWarnings("unused")
    private LogNamer() {}

    public LogNamer(String pattern)
    {
        this.pattern = pattern;
    }

    public abstract Path getFirstName(ZonedDateTime time) throws IOException;
    public abstract Path getNextName(Path currentLog, ZonedDateTime time) throws IOException;
    public abstract boolean shouldRotate(Path currentLog) throws IOException;
}
