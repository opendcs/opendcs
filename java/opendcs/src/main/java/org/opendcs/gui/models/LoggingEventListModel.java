package org.opendcs.gui.models;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;

import org.opendcs.logging.LoggingEvent;
import org.opendcs.logging.spi.LoggingEventProvider;
import org.opendcs.utils.logging.LoggingEventBuffer;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

public final class LoggingEventListModel extends AbstractListModel<LoggingEvent>
{
    final LoggingEventBuffer buffer;
    final List<LoggingEvent> events;
    final AtomicLong lastWrite = new AtomicLong(0L);

    public LoggingEventListModel()
    {
        // arguably there should only be one buffer shared by all instances of this model
        // but this currently works and future improvements should come later.
        buffer = new LoggingEventBuffer.Builder()
            .withProvider(LoggingEventProvider.getProvider())
            .build();
        events = buffer.getEvents();
        swapListeners();
        buffer.getPublisher().subscribe(new DefaultSubscriber<LoggingEvent>()
        {

            @Override
            public void onNext(@NonNull LoggingEvent t)
            {
                if (System.currentTimeMillis() - lastWrite.get() > 500L)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        int size = events.size();
                        LoggingEventListModel lelm = LoggingEventListModel.this;
                        lelm.fireIntervalAdded(lelm, size, size);
                        if (size >= buffer.getMaxSize())
                        {
                            lelm.fireIntervalRemoved(t, 0, 0);
                        }
                        lastWrite.set(System.currentTimeMillis());
                    });
                }

                request(1);
            }

            @Override
            public void onError(Throwable t)
            {
                request(1);
            }

            @Override
            public void onComplete()
            {
                /* don't care */
            }

        });
    }

    private void swapListeners()
    {
        ListDataListener[] listeners = this.getListDataListeners();
        for (ListDataListener listener: listeners)
        {
            this.removeListDataListener(listener);
        }
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
