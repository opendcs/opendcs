package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import decodes.tsdb.TsdbAppTemplate;

/**
 * Inform test extension that this
 * test needs a specific application running.
 */
@Documented
@Target({ElementType.METHOD,ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TsdbAppsRequired.class)
@ExtendWith(EnableIfAppsSupportedCondition.class)
public @interface TsdbAppRequired
{
    /**
     * Class used to create an instance of the application.
     * @return
     */
    Class<? extends TsdbAppTemplate> app();
    /**
     * Loading Application name for this instance.
     * @return
     */
    String appName();
}
