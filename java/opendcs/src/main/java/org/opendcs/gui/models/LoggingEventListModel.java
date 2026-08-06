package org.opendcs.gui.models;

import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.utils.logging.LoggingEventBuffer;

public final class LoggingEventListModel extends AbstractListModel<LoggingEvent>
{
    LoggingEventBuffer buffer;
    List<LoggingEvent> events;
    private volatile int previousSize = 0;

    public LoggingEventListModel()
    {
        // arguably there should only be one buffer shared by all instances of this model
        // but this currently works and future improvements should come later.
        buffer = new LoggingEventBuffer.Builder()
            .withProvider(LoggingEventProvider.getProvider())
            .build();
        events = buffer.getEvents();
        previousSize = events.size();
        final Thread t = new Thread(() -> 
        {
            while (true)
            {
                try
                {
                    final int currentSize = getSize();
                    if (currentSize > previousSize)
                    {
                        final int start = previousSize;
                        final int end = currentSize - 1;
                        SwingUtilities.invokeLater(() -> this.fireIntervalAdded(this, start, end));
                    }
                    else if (currentSize < previousSize)
                    {
                        final int start = currentSize;
                        final int end = previousSize - 1;
                        SwingUtilities.invokeLater(() -> this.fireIntervalRemoved(this, start, end));
                    }
                    previousSize = currentSize;
                    Thread.sleep(500);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }, "LoggingEventModel-update-thread");
        t.setDaemon(true);
        t.start();
    }

    public void setSize(int size)
    {
        buffer.setSize(size);
    }

    @Override
    public int getSize()
    {
        return events.size();
    }

    @Override
    public LoggingEvent getElementAt(int index)
    {
        return events.get(index);
    }
    
}
