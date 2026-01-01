package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opendcs.fixtures.spi.Configuration;

/**
* Enable the annotated test when the test's {@link org.opendcs.fixtures.spi.Configuration}
* indicates that REST API support is available.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnableIfApiSupported.EnableIfApiSupportedCondition.class)
public @interface EnableIfApiSupported
{
    class EnableIfApiSupportedCondition implements ExecutionCondition
    {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
        {
            Optional<Object> testInstance = ctx.getTestInstance();
            if (testInstance.isPresent())
            {
                List<Configuration> configs = AnnotationSupport.findAnnotatedFieldValues(testInstance.get(), ConfiguredField.class, Configuration.class);
                if (configs.size() == 1 && configs.get(0) != null)
                {
                    return configs.get(0).supportsRestApi() ? ConditionEvaluationResult.enabled("Rest API Support expected") : ConditionEvaluationResult.disabled("Rest Api Support not expected.");                
                }
                return ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class.");

            }
            return ConditionEvaluationResult.enabled("No determinative information at present level.");
        }
    }
}
