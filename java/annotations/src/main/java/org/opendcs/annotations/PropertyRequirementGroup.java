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
package org.opendcs.annotations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of related property requirements with a specific validation type.
 */
public class PropertyRequirementGroup implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * Enum defining the types of requirement validation
     */
    public enum GroupType
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
    
    private final String groupName;
    private final GroupType type;
    private final List<String> propertyNames;
    private String description;
    
    public PropertyRequirementGroup(String groupName, GroupType type, String description)
    {
        this.groupName = groupName;
        this.type = type;
        this.description = description;
        this.propertyNames = new ArrayList<>();
    }
    
    public PropertyRequirementGroup(String groupName, GroupType type)
    {
        this(groupName, type, "");
    }
    
    public void addProperty(String propertyName)
    {
        if (!propertyNames.contains(propertyName))
        {
            propertyNames.add(propertyName);
        }
    }
    
    public boolean isSatisfied(Map<String, String> providedProperties)
    {
        List<String> providedNames = new ArrayList<>();
        for (String propName : propertyNames)
        {
            String value = providedProperties.get(propName);
            if (value != null && !value.trim().isEmpty())
            {
                providedNames.add(propName);
            }
        }
        
        int count = providedNames.size();
        
        switch (type)
        {
            case ONE_OF:
                return count == 1;
            case ALL_OR_NONE:
                return count == 0 || count == propertyNames.size();
            case AT_LEAST_ONE:
                return count >= 1;
            case ALL_REQUIRED:
            case INDIVIDUAL:
                return count == propertyNames.size();
            default:
                return false;
        }
    }
    
    public String getValidationError(Map<String, String> providedProperties)
    {
        if (isSatisfied(providedProperties))
        {
            return null;
        }
        
        List<String> missing = new ArrayList<>();
        List<String> provided = new ArrayList<>();
        
        for (String propName : propertyNames)
        {
            String value = providedProperties.get(propName);
            if (value != null && !value.trim().isEmpty())
            {
                provided.add(propName);
            }
            else
            {
                missing.add(propName);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Requirement group '").append(groupName).append("' ");
        
        switch (type)
        {
            case ONE_OF:
                if (provided.isEmpty())
                {
                    sb.append("requires exactly one of: ").append(propertyNames);
                }
                else
                {
                    sb.append("requires exactly one property, but found ").append(provided.size())
                      .append(" (").append(provided).append(")");
                }
                break;
                
            case ALL_OR_NONE:
                sb.append("requires all or none of: ").append(propertyNames)
                  .append(", but only ").append(provided.size()).append(" provided");
                break;
                
            case AT_LEAST_ONE:
                sb.append("requires at least one of: ").append(propertyNames);
                break;
                
            case ALL_REQUIRED:
                sb.append("requires all of: ").append(propertyNames)
                  .append(", missing: ").append(missing);
                break;
                
            case INDIVIDUAL:
                sb.append("requires property: ").append(propertyNames.get(0));
                break;
                
            default:
                sb.append("has unknown requirement type");
        }
        
        return sb.toString();
    }
    
    public String getDescription()
    {
        if (description != null && !description.isEmpty())
            return description;
            
        switch (type)
        {
            case ONE_OF:
                return "Exactly one property from this group is required";
            case ALL_OR_NONE:
                return "Either all properties in this group must be provided, or none";
            case AT_LEAST_ONE:
                return "At least one property from this group is required";
            case ALL_REQUIRED:
                return "All properties in this group are required";
            case INDIVIDUAL:
                return "This property is individually required";
            default:
                return "Unknown requirement type";
        }
    }
    
    public String getGroupName()
    {
        return groupName;
    }
    
    public GroupType getType()
    {
        return type;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public String getName()
    {
        return groupName;
    }
    
    public List<String> getPropertyNames()
    {
        return new ArrayList<>(propertyNames);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PropertyRequirementGroup other = (PropertyRequirementGroup) obj;
        return groupName.equals(other.groupName);
    }
    
    @Override
    public int hashCode()
    {
        return groupName.hashCode();
    }
}