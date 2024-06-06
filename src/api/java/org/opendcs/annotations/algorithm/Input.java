package org.opendcs.annotations.algorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Input
{
    /**
     * Leave the name blank will use the name of the field.
     * @return
     */
    String name() default "";
    Class<?> type() default Double.class;
    String typeCode() default "i";
}
