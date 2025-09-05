package org.opendcs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation for defining requirement groups for algorithm properties.
 * This provides a cleaner way to define groups without repetition on each property.
 * 
 * Example usage:
 * @AlgorithmRequirements(
 *     groups = {
 *         @RequirementGroup(name = "auth", type = RequirementType.ONE_OF, 
 *                          properties = {"username", "apiKey"}),
 *         @RequirementGroup(name = "location", type = RequirementType.AT_LEAST_ONE,
 *                          properties = {"latitude", "longitude"})
 *     }
 * )
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyRequirements
{
    /**
     * Array of requirement group definitions
     */
    RequirementGroup[] groups() default {};
    
    /**
     * Defines a requirement group
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface RequirementGroup
    {
        /**
         * Name of the group
         */
        String name();
        
        /**
         * Type of requirement
         */
        RequirementType type();
        
        /**
         * Properties in this group
         */
        String[] properties();
        
        /**
         * Optional description
         */
        String description() default "";
    }

    /**
     * Enum defining the types of requirement validation
     */
    public enum RequirementType
    {
        /** Exactly one property in the group must be satisfied */
        ONE_OF,
        /** All properties in the group must be satisfied, or none */
        ALL_OR_NONE,
        /** At least one property in the group must be satisfied */
        AT_LEAST_ONE,
        /** All properties in the group must be satisfied */
        ALL_REQUIRED,
        /** Individual property requirement */
        INDIVIDUAL
    }
}