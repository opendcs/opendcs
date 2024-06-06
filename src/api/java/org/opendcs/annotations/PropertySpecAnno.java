package org.opendcs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertySpecAnno
{
    /**
     * Leaving the name blank will use the name of the field.
     * @return
     */
    String name() default "";
    String value() default "";
    String propertySpecType() default "s";
    String description() default "";
}
