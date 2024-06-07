package org.opendcs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertySpec
{
    /**
     * Leaving the name blank will use the name of the field.
     * @return
     */
    String name() default "";
    /**
     * Regardless of actual type the default value is presented as a string
     * @return
     */
    String value() default "";
    String propertySpecType() default "s";
    String description() default "";
}
