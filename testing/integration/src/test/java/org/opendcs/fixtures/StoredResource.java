package org.opendcs.fixtures;

import java.io.Closeable;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.ExtensionContext;

public class StoredResource<T> implements ExtensionContext.Store.CloseableResource
{
    private T resource;
    private Consumer<T> onClose;

    public StoredResource(T resource, Consumer<T> onClose)
    {
        this.resource = resource;
        this.onClose = onClose;
    }

    @Override
    public void close() throws Throwable
    {
        StackTraceElement stes[] = Thread.currentThread().getStackTrace();
        for(StackTraceElement ste: stes) {
            System.out.println(ste.toString());
        }
        onClose.accept(resource);        
    }
    
}
