package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation will be used by the JVM to hold
 * all configured {@link TsdbAppRequired} instances.
 *
 * It can be used directory but such usage is not recommended. Multiple
 * separate {@link TsdbAppRequired} annotations are the preferred method.
 */
@Documented
@Target({ElementType.METHOD,ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TsdbAppsRequired
{
    TsdbAppRequired[] value();
}
