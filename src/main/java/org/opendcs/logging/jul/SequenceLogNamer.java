package org.opendcs.logging.jul;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

public class SequenceLogNamer extends LogNamer
{
    private int maxFiles;
    private int maxFileSize;
    private Path currentLogName;

    public SequenceLogNamer(String pattern, int maxFiles, int maxFileSize)
    {
        super(pattern);
        this.currentLogName = Paths.get(pattern.replace("%g",""));
        this.maxFiles = maxFiles;
        this.maxFileSize = maxFileSize;
    }

    /**
     * time is ignored in this implementation
     */
    @Override
    public Path getFirstName(ZonedDateTime time) throws IOException
    {
        Path path = currentLogName;
        if(Files.exists(path))
        {
            path = getNextName(path, null);
        }
        return path;
    }

    /**
     * @param time ignored in this implementation
     * @return the next filename in the sequence for the handler to open.
     */
    @Override
    public Path getNextName(Path currentLog, ZonedDateTime time) throws IOException
    {
        if (Files.exists(currentLogName) && shouldRotate(currentLogName))
        {
            for(int i = maxFiles-1; i >= 0; i--)
            {
                Path file = fileForSequence(pattern, i);
                if(Files.exists(file) && (i == maxFiles -1))
                {
                    Files.delete(file);
                }
                else if(Files.exists(file))
                {
                    Files.move(file,fileForSequence(pattern,i+1));
                }
            }
            if (Files.exists(currentLogName))
            {
                Files.move(currentLogName,fileForSequence(pattern, 0));
            }
        }
        return currentLogName;
    }

    private Path fileForSequence(String pattern, int sequence)
    {
        return Paths.get(String.format(pattern.replace("%g","%d"),sequence));
    }

    @Override
    public boolean shouldRotate(Path currentLog) throws IOException
    {
        return Files.size(currentLog) >= maxFileSize;
    }
    
}
