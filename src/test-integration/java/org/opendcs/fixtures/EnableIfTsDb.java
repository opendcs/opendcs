package org.opendcs.fixtures;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfSql;
import org.opendcs.spi.configuration.Configuration;

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
    static class EnableIfTsDbCondition implements ExecutionCondition
    {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
        {
            Object testInstance = ctx.getRequiredTestInstance();
            List<Configuration> configs = AnnotationSupport.findAnnotatedFieldValues(testInstance, ConfiguredField.class, Configuration.class);
            if (configs.size() == 1 && configs.get(0) != null) {
                return configs.get(0).isTsdb() ? ConditionEvaluationResult.enabled("Is Timeseries Db") : ConditionEvaluationResult.disabled("Not a Timeseries Db");
            }
            return ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class.");
        }
    }
}