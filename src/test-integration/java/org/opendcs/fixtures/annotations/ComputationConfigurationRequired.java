package org.opendcs.fixtures.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify a list of configuration xml files that need to 
 * be imported before any tests in this test class are run.
 * 
 * Values that start with $ will have {@code EnvExpander.expand(string,System.getProperties())}
 * run on them to expand any properties; the properties used will be those set by the configuration and 
 * passed in to the integration-test junitlauncher task.
 * 
 * Other wise values are relative to @see TestResources.resourceDir
 */
@Documented
@Target({ElementType.METHOD,ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ComputationConfigurationRequired
{
    /**
     * List of resource file values
     */
    String[] value();
}
