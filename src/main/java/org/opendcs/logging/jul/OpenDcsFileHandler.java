package org.opendcs.logging.jul;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Custom log namer for use in OpenDCS to achieve existing and additional
 * functionality. Files can be specified by date, or sequence
 */
public class OpenDcsFileHandler extends Handler
{
    private Path currentLogFile;
    private OutputStream outputStream = null;
    private LogNamer logNamer;

    /**
     * Determine how our log files are named
     * @param fileNamePattern can contain %g for sequence number, or java date formatting for dated files
     * @param maxFiles
     * @param maxSize
     */
    public OpenDcsFileHandler(String fileNamePattern, int maxSize, int maxFiles) throws IOException
    {
        super();
        this.logNamer = setupLogNamer(fileNamePattern, maxFiles, maxSize);
        openLog();
    }

    private LogNamer setupLogNamer(String fileNamePattern, int maxFiles, int maxSize)
    {
        LogNamer ret = null;
        if (!fileNamePattern.contains("%"))
        {
            ret = new StaticLogName(fileNamePattern);
        }
        else if(fileNamePattern.contains("%g"))
        {
            ret = new SequenceLogNamer(fileNamePattern, maxFiles, maxSize);
        }
        return ret;
    }

    @Override
    public void publish(LogRecord record) 
    {
        if (!isLoggable(record))
        {
            return;
        }

        checkLogRotation(record);
        Formatter fmt = this.getFormatter();
        String msg = fmt.format(record);
        try
        {
            outputStream.write(msg.getBytes());
        }
        catch(IOException ex)
        {
            this.reportError("Unable to write log data", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    private void checkLogRotation(LogRecord record)
    {
        try
        {
            if(!logNamer.shouldRotate(currentLogFile))
            {
                return;
            }
            openLog();
        }
        catch(Exception ex)
        {
            System.err.println(ex.getLocalizedMessage());
            getErrorManager().error("Unable to rotate log", ex, ErrorManager.OPEN_FAILURE);
        }
    }

    @Override
    public void flush()
    {
        try
        {
            outputStream.flush();
        }
        catch (IOException ex)
        {
            reportError("Unable to flush stream.", ex, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public void close() throws SecurityException
    {
        try {
            outputStream.close();
        } catch (IOException ex) {
            reportError("Unable to close output stream.", ex, ErrorManager.CLOSE_FAILURE);
        }
    }
    

    private void openLog() throws IOException
    {
        if (outputStream != null)
        {
            outputStream.close();
        }
        
        currentLogFile = logNamer.getNextName(currentLogFile, ZonedDateTime.now());
        outputStream = new FileOutputStream(currentLogFile.toFile(), true);
    }
}
