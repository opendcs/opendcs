package org.opendcs.gui.models;

import java.util.List;

import javax.swing.AbstractListModel;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.utils.logging.LoggingEventBuffer;

public final class LoggingEventListModel extends AbstractListModel<LoggingEvent>
{
    LoggingEventBuffer buffer;
    List<LoggingEvent> events;

    public LoggingEventListModel()
    {
        
        buffer = new LoggingEventBuffer.Builder()
            .withProvider(LoggingEventProvider.getProvider())
            .build();
        events = buffer.getEvents();
        final Thread t = new Thread(() -> 
        {
            while (true)
            {
                try
                {
                    this.fireContentsChanged(this, 0, getSize());
                    Thread.sleep(100);
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
