package org.opendcs.fixtures.annotations;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opendcs.fixtures.configuration.Configuration;

/**
 * Simple check to help avoid methods getting called in implementations
 * that don't support the requirements.
 */
public class EnableIfAppsSupportedCondition implements ExecutionCondition
{
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
    {
        Optional<AnnotatedElement> appAnnotation = ctx.getElement();
        if (appAnnotation.isPresent())
        {
            AnnotatedElement a = appAnnotation.get();
            TsdbAppRequired appRequired = a.getAnnotation(TsdbAppRequired.class);
            Object testInstance = ctx.getRequiredTestInstance();
            List<Configuration> configs = AnnotationSupport.findAnnotatedFieldValues(testInstance, ConfiguredField.class, Configuration.class);
            if (configs.size() == 1 && configs.get(0) != null) {
                return configs.get(0).implementsSupportFor(appRequired.app()) 
                    ? ConditionEvaluationResult.enabled("Implementation Supports " + appRequired.app().getName() )
                    : ConditionEvaluationResult.disabled("Implementation Does not Support " + appRequired.app().getName());
            }    
        }
        
        return ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class.");
    }
}
