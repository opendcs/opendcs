/**
 * Copyright 2025 The OpenDCS Consortium and contributors
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
package org.opendcs.annotations.api;

import org.opendcs.annotations.PropertyRequirements.RequirementType;

import org.opendcs.annotations.PropertyRequirements;
import org.opendcs.annotations.PropertySpec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for working with PropertySpec annotations and requirement groups.
 */
public class PropertySpecHelper
{
    /**
     * Extracts requirement groups from PropertySpec annotations on a class.
     */
    public static Map<String, PropertyRequirementGroup> extractRequirementGroups(Class<?> clazz)
    {
        Map<String, PropertyRequirementGroup> groups = new HashMap<>();
        
        // First, process class-level requirements
        PropertyRequirements classReqs = clazz.getAnnotation(PropertyRequirements.class);
        if (classReqs != null)
        {
            for (PropertyRequirements.RequirementGroup groupDef : classReqs.groups())
            {
                RequirementType type = convertAlgorithmRequirementType(groupDef.type());
                PropertyRequirementGroup group = new PropertyRequirementGroup(
                    groupDef.name(),
                    type,
                    groupDef.description().isEmpty() ? getDefaultDescription(type) : groupDef.description()
                );
                
                for (String prop : groupDef.properties())
                {
                    group.addProperty(prop);
                }
                groups.put(group.getName(), group);
            }
        }
        
        // Then process field-level requirements
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields)
        {
            PropertySpec spec = field.getAnnotation(PropertySpec.class);
            if (spec != null)
            {
                String propertyName = getPropertyName(field, spec);
                
                if (spec.required())
                {
                    // Simple required=true creates an individual requirement group
                    String groupName = "_required_" + propertyName;
                    PropertyRequirementGroup group = new PropertyRequirementGroup(
                        groupName,
                        RequirementType.INDIVIDUAL,
                        "Property '" + propertyName + "' is required"
                    );
                    group.addProperty(propertyName);
                    groups.put(groupName, group);
                }
            }
        }
        
        return groups;
    }
    
    /**
     * Creates requirement groups from PropertySpec annotation.
     */
    public static List<PropertyRequirementGroup> createRequirementGroups(String propertyName, PropertySpec spec)
    {
        List<PropertyRequirementGroup> groups = new ArrayList<>();
        
        if (spec.required())
        {
            PropertyRequirementGroup group = new PropertyRequirementGroup(
                "_required_" + propertyName,
                RequirementType.INDIVIDUAL,
                "Property '" + propertyName + "' is required"
            );
            group.addProperty(propertyName);
            groups.add(group);
        }
        
        return groups;
    }
    
    /**
     * Determines the property name from field and annotation.
     */
    public static String getPropertyName(Field field, PropertySpec spec)
    {
        String name = spec.name();
        if (name == null || name.isEmpty())
        {
            name = field.getName();
        }
        return name;
    }
    
    /**
     * Checks if a property is required based on its annotation.
     */
    public static boolean isRequired(PropertySpec spec)
    {
        return spec.required();
    }
    
    /**
     * Gets the property type string from annotation and field type.
     */
    public static String getPropertyType(PropertySpec spec, Field field)
    {
        String specType = spec.propertySpecType();
        if (specType != null && !specType.isEmpty())
        {
            return specType;
        }
        
        Class<?> fieldType = field.getType();
        
        if (fieldType == Boolean.class || fieldType == boolean.class)
            return "b";
        else if (fieldType == Integer.class || fieldType == int.class || 
                 fieldType == Long.class || fieldType == long.class)
            return "i";
        else if (fieldType == Float.class || fieldType == float.class || 
                 fieldType == Double.class || fieldType == double.class)
            return "n";
        else if (fieldType.isEnum())
            return "E:" + fieldType.getName();
        else
            return "s";
    }
    
    /**
     * Convert AlgorithmRequirements.RequirementType to PropertyRequirementGroup.GroupType
     */
    private static RequirementType convertAlgorithmRequirementType(
            PropertyRequirements.RequirementType type)
    {
        switch (type)
        {
            case ONE_OF:
                return RequirementType.ONE_OF;
            case ALL_OR_NONE:
                return RequirementType.ALL_OR_NONE;
            case AT_LEAST_ONE:
                return RequirementType.AT_LEAST_ONE;
            case ALL_REQUIRED:
                return RequirementType.ALL_REQUIRED;
            case INDIVIDUAL:
                return RequirementType.INDIVIDUAL;
            default:
                return RequirementType.INDIVIDUAL;
        }
    }
    
    /**
     * Get default description for a requirement type
     */
    private static String getDefaultDescription(RequirementType type)
    {
        switch (type)
        {
            case ONE_OF:
                return "Exactly one of these properties must be provided";
            case ALL_OR_NONE:
                return "All or none of these properties must be provided";
            case AT_LEAST_ONE:
                return "At least one of these properties must be provided";
            case ALL_REQUIRED:
                return "All of these properties are required";
            case INDIVIDUAL:
                return "This property is individually required";
            default:
                return "";
        }
    }
}