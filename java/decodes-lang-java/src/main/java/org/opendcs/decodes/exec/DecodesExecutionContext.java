package org.opendcs.decodes.exec;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;

public interface DecodesExecutionContext<T>
{
    /**
     * Advance the current position by delta amount.
     * 
     * @param delta number of bytes to move, can be negative to go backwards.
     * @return the new absolute position
     */
    default int advancePosition(int delta)
    {
        var buffer = getMessageData();
        int current = buffer.position();
        buffer.position(current + delta);
        return buffer.position();
    }

    /**
     * Retrieve the current position in the byte buffer.
     * @return
     */
    default int currentPosition()
    {
        return getMessageData().position();
    }
    
    /**
     * Retrive the byte buffer view to retrieve data.
     * @return
     */
    ByteBuffer getMessageData();

    /**
     * Build an appropriate data collection. This engine makes no determination about what the
     * contents and type of that should be. Implement this and addVariable to retrieve 
     * the required elements.
     * @return
     */
    T getDataCollection();

    /**
     * Add a new determined time based variable to the stored collection.
     * @param zdt Current Date Time, with time zone
     * @param value value, usually a double but any type your DataCollection implementation supports
     *              can be used.
     */
    void addVariable(ZonedDateTime zdt, Object value); // maybe expand this out
}
