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
package org.opendcs.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates properties against PropertySpec annotations and their requirement groups.
 */
public class PropertySpecValidator
{
    private final Map<String, PropertyRequirementGroup> requirementGroups;
    
    public PropertySpecValidator(Class<?> clazz)
    {
        this.requirementGroups = PropertySpecHelper.extractRequirementGroups(clazz);
    }
    
    public PropertySpecValidator(Map<String, PropertyRequirementGroup> requirementGroups)
    {
        this.requirementGroups = requirementGroups;
    }
    
    public ValidationResult validate(Map<String, String> providedProperties)
    {
        List<ValidationError> errors = new ArrayList<>();
        boolean isValid = true;
        
        for (PropertyRequirementGroup group : requirementGroups.values())
        {
            if (!group.isSatisfied(providedProperties))
            {
                String errorMessage = group.getValidationError(providedProperties);
                errors.add(new ValidationError(group.getGroupName(), errorMessage));
                isValid = false;
            }
        }
        
        return new ValidationResult(isValid, errors);
    }
    
    public static ValidationResult validateClass(Class<?> clazz, Map<String, String> providedProperties)
    {
        PropertySpecValidator validator = new PropertySpecValidator(clazz);
        return validator.validate(providedProperties);
    }
    
    public boolean isPropertyRequired(String propertyName)
    {
        String nameUpper = propertyName.toUpperCase();
        for (PropertyRequirementGroup group : requirementGroups.values())
        {
            for (String propName : group.getPropertyNames())
            {
                if (propName.equalsIgnoreCase(nameUpper))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    public List<PropertyRequirementGroup> getGroupsForProperty(String propertyName)
    {
        List<PropertyRequirementGroup> groups = new ArrayList<>();
        String nameUpper = propertyName.toUpperCase();
        for (PropertyRequirementGroup group : requirementGroups.values())
        {
            for (String propName : group.getPropertyNames())
            {
                if (propName.equalsIgnoreCase(nameUpper))
                {
                    groups.add(group);
                    break;
                }
            }
        }
        return groups;
    }
    
    public boolean isMissingPropertyViolatingRequirements(String propertyName, Map<String, String> providedProperties)
    {
        String value = providedProperties.get(propertyName);
        if (value != null && !value.trim().isEmpty())
        {
            return false;
        }
        
        List<PropertyRequirementGroup> groups = getGroupsForProperty(propertyName);
        if (groups.isEmpty())
        {
            return false;
        }
        
        for (PropertyRequirementGroup group : groups)
        {
            if (!group.isSatisfied(providedProperties))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public static class ValidationResult
    {
        private final boolean valid;
        private final List<ValidationError> errors;
        
        public ValidationResult(boolean valid, List<ValidationError> errors)
        {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public boolean isValid()
        {
            return valid;
        }
        
        public List<ValidationError> getErrors()
        {
            return errors;
        }
        
        public String getErrorMessage()
        {
            if (errors.isEmpty())
                return "";
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errors.size(); i++)
            {
                if (i > 0) sb.append("; ");
                sb.append(errors.get(i).getMessage());
            }
            return sb.toString();
        }
    }
    
    public static class ValidationError
    {
        private final String groupName;
        private final String message;
        
        public ValidationError(String groupName, String message)
        {
            this.groupName = groupName;
            this.message = message;
        }
        
        public String getGroupName()
        {
            return groupName;
        }
        
        public String getMessage()
        {
            return message;
        }
    }
}