package org.opendcs.lrgs.http.dds;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * For resources that will need access to a valid DDS Session to retrieve
 * data from the archive.
 * UseDdsSession
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDdsSession
{
    String KEY = "ddsSession";
}
