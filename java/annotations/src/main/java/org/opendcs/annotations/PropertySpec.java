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
     * If true and no requirementGroup is specified, the property is placed in its own unique group.
     * @return true if this property is required
     */
    boolean required() default false;
    
    /**
     * Specifies the requirement group this property belongs to.
     * Properties in the same group are mutually exclusive - only one is required.
     * Empty string means the property is not part of any requirement group (optional).
     *
     * @return the requirement group name, or empty string if optional
     */
    String requirementGroup() default "";
}
