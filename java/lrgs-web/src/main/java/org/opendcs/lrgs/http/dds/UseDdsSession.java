package org.opendcs.lrgs.http.dds;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * For resources that will need access to a valid DDS Session to retrieve
 * data from the archive.
 *
 * Any jakarta.ws.rs Resource that will interact with the Message Archive
 * should be marked with this annotation to ensure an {@link lrgs.common.DcpMsgRetriever}
 * instance avaiable.
 *
 * UseDdsSession
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDdsSession
{
    String KEY = "ddsSession";
}
