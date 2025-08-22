/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PropertySpec
{
    /**
     * Leaving the name blank will use the name of the annotated field.
     * @return the field name
     */
    String name() default "";
    /**
     * Regardless of actual type the default value is presented as a string
     * @return the default property value as a string.
     */
    String value() default "";
    String propertySpecType() default "";
    String description() default "";
    
    /**
     * Marks this property as required. 
     * If true and no requirementGroups are specified, the property is placed in its own unique group.
     * @return true if this property is required
     */
    boolean required() default false;
    
    /**
     * Specifies the requirement groups this property belongs to.
     * A property can belong to multiple requirement groups with different validation types.
     * Empty array means the property is not part of any requirement group (optional).
     * If required=true and requirementGroups is empty, a unique individual group is created.
     *
     * @return array of requirement group definitions
     */
    RequirementGroupDef[] requirementGroups() default {};
    
    /**
     * Annotation to define a requirement group
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})  // This annotation is only used within PropertySpec
    public @interface RequirementGroupDef
    {
        /**
         * The name of the requirement group
         * @return group name
         */
        String name();
        
        /**
         * The type of requirement validation for this group
         * @return requirement type
         */
        RequirementGroupType type() default RequirementGroupType.ONE_OF;
        
        /**
         * Optional description for this group
         * @return group description
         */
        String description() default "";
    }
    
    /**
     * Enum defining the types of requirement validation
     */
    public enum RequirementGroupType
    {
        /** Exactly one property in the group must be satisfied */
        ONE_OF,
        /** All properties in the group must be satisfied, or none */
        ALL_OR_NONE,
        /** At least one property in the group must be satisfied */
        AT_LEAST_ONE,
        /** All properties in the group must be satisfied */
        ALL_REQUIRED,
        /** Individual property requirement (for backwards compatibility) */
        INDIVIDUAL
    }
}
