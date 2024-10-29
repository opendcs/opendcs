package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opendcs.fixtures.configuration.Configuration;

/**
 * Only run this test if the database under test is a SQL based database
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@EnableIfSql
@ExtendWith(EnableIfTsDb.EnableIfTsDbCondition.class)
public @interface EnableIfTsDb
{
    public String impl() default "";

    static class EnableIfTsDbCondition implements ExecutionCondition
    {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
        {
            ConditionEvaluationResult retVal = ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class.");
            Object testInstance = ctx.getRequiredTestInstance();
            List<Configuration> configs = AnnotationSupport.findAnnotatedFieldValues(testInstance, ConfiguredField.class, Configuration.class);
            if (configs.size() == 1 && configs.get(0) != null)
            {
                boolean isTsDb = configs.get(0).isTsdb();
                if (!isTsDb)
                {
                    retVal = ConditionEvaluationResult.disabled("Not a Timeseries Db");
                }
                else
                {
                    Optional<AnnotatedElement> element = ctx.getElement();
                    EnableIfTsDb anno = element.get().getAnnotation(EnableIfTsDb.class);
                    if (anno.impl().isEmpty())
                    {
                        retVal = ConditionEvaluationResult.enabled("Is Timeseries Db");
                    }
                    else if (anno.impl().equalsIgnoreCase(configs.get(0).getName()))
                    {
                        retVal = ConditionEvaluationResult.enabled("Is Timeseries Db of type " + anno.impl());
                    }
                    else
                    {
                        retVal = ConditionEvaluationResult.disabled("Not a Timeseries Db of type" + anno.impl() + " was " +configs.get(0).getName());
                    }
                }
            }
            return retVal;
        }
    }
}