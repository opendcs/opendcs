package org.opendcs.fixtures.extensions.lrgs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LrgsConfig
{
    /**
     * Additional configuration properties for the LRGS
     * separated by new lines
     * @return
     */
    String value();
}
