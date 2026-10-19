package org.opendcs.data;

import java.time.ZonedDateTime;

/**
 * For any DTO, or DTO operation that would expect to know or display 
 * create and update time information.
 * TracksChanges
 */
public interface TracksChanges
{
    /**
     * @return When this type was first created.      
     */
    ZonedDateTime createdAt();
    /**
     * @return When this type was last modified.
     */
    ZonedDateTime updatedAt();
}
