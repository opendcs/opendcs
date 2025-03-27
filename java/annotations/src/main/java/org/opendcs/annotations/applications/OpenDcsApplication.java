package org.opendcs.annotations.applications;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OpenDcsApplication
{
    /**
     * OpenDCS Application name. Used to create script file name.
     * Class name will be used by default.
     * @return
     */
    String appName() default "";
    /**
     * Any environment variables that should be captured and set as
     * java properties.
     * @return
     */
    String[] environmentAsProperty() default {};
}
