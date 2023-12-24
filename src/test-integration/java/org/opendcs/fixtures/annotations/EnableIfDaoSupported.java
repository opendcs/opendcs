package org.opendcs.fixtures.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
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
import org.opendcs.spi.configuration.Configuration;

import opendcs.dao.DaoBase;

/**
 * Only run this test if the database under test is a SQL based database
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnableIfDaoSupported.EnableIfDaoSupportedCondition.class)
public @interface EnableIfDaoSupported
{
    public Class<? extends DaoBase>[] value();

    public static class EnableIfDaoSupportedCondition implements ExecutionCondition
    {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext ctx)
        {
            Optional<AnnotatedElement> daoAnnotation = ctx.getElement();
            if (daoAnnotation.isPresent())
            {
                AnnotatedElement daoElement = daoAnnotation.get();
                EnableIfDaoSupported daoAnno = daoElement.getAnnotation(EnableIfDaoSupported.class);
                Object testInstance = ctx.getRequiredTestInstance();
                List<Configuration> configs = AnnotationSupport.findAnnotatedFieldValues(testInstance, ConfiguredField.class, Configuration.class);
                if (configs.size() == 1 && configs.get(0) != null) 
                {
                    final Configuration config = configs.get(0);
                    for(Class<? extends DaoBase> daoClass: daoAnno.value())
                    {
                        if (!config.supportsDao(daoClass))
                        {
                            return ConditionEvaluationResult.disabled("Database does not support required DAO: " + daoClass.getName());
                        }
                    }
                    return ConditionEvaluationResult.enabled("Database supports required DAOs.");
                }
                else
                {
                    return ConditionEvaluationResult.disabled("No " + Configuration.class.getName() + " member fields present in Test class to check compability.");
                }
            }
            return ConditionEvaluationResult.disabled("No EnabledifDaoSupportCondition presented.");
        }
    }
}
