package org.opendcs.lrgs.http.dds;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface UseDdsSession
{
    final String KEY = "ddsSession";
}
