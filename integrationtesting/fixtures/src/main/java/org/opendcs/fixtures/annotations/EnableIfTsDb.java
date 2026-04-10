package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opendcs.fixtures.spi.Configuration;

/**
 * Only run this test if the database under test is a SQL based database
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnableIfTsDb.EnableIfTsDbCondition.class)
public @interface EnableIfTsDb
{
    String ENGINE = System.getProperty("opendcs.test.engine");
    String[] value() default {};

    class EnableIfTsDbCondition implements ExecutionCondition
    {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
        {
            ConditionEvaluationResult retVal = ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class.");
            var element = ctx.getElement();
            if (element.isPresent())
            {
                EnableIfTsDb anno = element.get().getAnnotation(EnableIfTsDb.class);
                if ((anno == null || anno.value().length == 0) && !"OpenDCS-XML".equals(ENGINE))
                {
                    retVal = ConditionEvaluationResult.enabled("Is Timeseries Db");
                }
                else if (matches(anno.value(),ENGINE))
                {
                    retVal = ConditionEvaluationResult.enabled(
                        String.format("Is Timeseries Db of type (%s)", String.join(",", anno.value()))
                    );
                }
                else
                {
                    retVal = ConditionEvaluationResult.disabled(
                        String.format("Not a Timeseries Db of type (%s) was %s", String.join(",", anno.value()), ENGINE));
                }
            }
            return retVal;
        }

        private boolean matches(String[] allowed, String have)
        {
            for (String current: allowed)
            {
                if (current.equals(have))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
