package org.opendcs.annotations.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectDao
{
    /**
     * Some systems may be able to engage different behavior if a given DAO is not
     * present. If true, this allows the DAO requesting injection to not fail
     * if a required DAO can't be obtained.
     * @return
     */
    boolean optional() default false;
}
