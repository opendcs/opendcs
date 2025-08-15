package decodes.util;

import ilex.util.LoadResourceBundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import decodes.util.DecodesSettings;

/**
 * Represents a group of related property requirements with a specific validation type.
 * This allows for complex requirement relationships between properties.
 */
public class RequirementGroup implements Serializable
{
    /** The unique name of this requirement group */
    private String groupName;
    
    /** The type of requirement validation for this group */
    private RequirementType type;
    
    /** List of property names that belong to this group */
    private List<String> propertyNames;
    
    /** Optional description of this requirement group */
    private String description;

    private ResourceBundle compeditDescriptions = null;
    
    /**
     * Enum defining the types of requirement validation
     */
    public enum RequirementType
    {
        /** Exactly one property in the group must be satisfied */
        ONE_OF("One property from this group is required"),
        
        /** All properties in the group must be satisfied, or none */
        ALL_OR_NONE("Either all properties in this group must be provided, or none"),
        
        /** At least one property in the group must be satisfied */
        AT_LEAST_ONE("At least one property from this group is required"),
        
        /** All properties in the group must be satisfied */
        ALL_REQUIRED("All properties in this group are required"),
        
        /** Individual property requirement (for backwards compatibility) */
        INDIVIDUAL("This property is individually required");
        
        private final String description;
        
        RequirementType(String description)
        {
            this.description = description;
        }
        
        public String getDescription()
        {
            return description;
        }
    }
    
    /**
     * Default constructor
     */
    public RequirementGroup()
    {
        this.propertyNames = new ArrayList<>();
        this.type = RequirementType.ONE_OF; // Default
    }
    
    /**
     * Constructor with group name and type
     * @param groupName The unique name of this group
     * @param type The requirement validation type
     */
    public RequirementGroup(String groupName, RequirementType type)
    {
        this.groupName = groupName;
        this.type = type;
        this.propertyNames = new ArrayList<>();

        DecodesSettings settings = DecodesSettings.instance();
        this.compeditDescriptions =  LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/compedit",
                settings.language);
    }
    
    /**
     * Constructor with all parameters
     * @param groupName The unique name of this group
     * @param type The requirement validation type
     * @param description Optional description
     */
    public RequirementGroup(String groupName, RequirementType type, String description)
    {
        this(groupName, type);
        this.description = description;
    }
    
    /**
     * Add a property to this requirement group
     * @param propertyName The name of the property to add
     */
    public void addProperty(String propertyName)
    {
        if (!propertyNames.contains(propertyName))
        {
            propertyNames.add(propertyName);
        }
    }
    
    /**
     * Remove a property from this requirement group
     * @param propertyName The name of the property to remove
     */
    public void removeProperty(String propertyName)
    {
        propertyNames.remove(propertyName);
    }
    
    /**
     * Check if a property belongs to this group
     * @param propertyName The name of the property to check
     * @return true if the property is in this group
     */
    public boolean containsProperty(String propertyName)
    {
        return propertyNames.contains(propertyName);
    }
    
    /**
     * Validate if the provided properties satisfy this requirement group
     * @param providedProperties List of property names that have been provided
     * @return true if the requirement is satisfied, false otherwise
     */
    public boolean isSatisfied(List<String> providedProperties)
    {
        int count = 0;
        for (String propName : propertyNames)
        {
            if (providedProperties.contains(propName))
            {
                count++;
            }
        }
        
        switch (type)
        {
            case ONE_OF:
                return count == 1;
                
            case ALL_OR_NONE:
                return count == 0 || count == propertyNames.size();
                
            case AT_LEAST_ONE:
                return count >= 1;
                
            case ALL_REQUIRED:
                return count == propertyNames.size();
                
            case INDIVIDUAL:
                // For individual requirements, all properties must be provided
                return count == propertyNames.size();
                
            default:
                return false;
        }
    }
    
    /**
     * Get a validation error message for this requirement group
     * @param providedProperties List of property names that have been provided
     * @return Error message or null if requirement is satisfied
     */
    public String getValidationError(List<String> providedProperties)
    {
        if (isSatisfied(providedProperties))
        {
            return null;
        }
        
        int count = 0;
        List<String> missing = new ArrayList<>();
        List<String> provided = new ArrayList<>();
        
        for (String propName : propertyNames)
        {
            if (providedProperties.contains(propName))
            {
                provided.add(propName);
                count++;
            }
            else
            {
                missing.add(propName);
            }
        }
        
        switch (type)
        {
            case ONE_OF:
                if (count == 0)
                {
                    return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorOneOfNone"),
                        groupName, propertyNames);
                }
                else
                {
                    return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorOneOfMultiple"), 
                        groupName, propertyNames, provided);
                }
                
            case ALL_OR_NONE:
                return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorAllOrNone"), 
                    groupName, propertyNames, provided);
                
            case AT_LEAST_ONE:
                return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorAtLeastOne"), 
                    groupName, propertyNames);
                
            case ALL_REQUIRED:
                return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorAllRequired"), 
                    groupName, propertyNames, missing);
                
            case INDIVIDUAL:
                return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorIndividual"), 
                    missing.isEmpty() ? groupName : missing.get(0));
                
            default:
                return String.format(compeditDescriptions.getString("ComputationsEditPanel.errorDefault"), groupName);
        }
    }
    
    // Getters and setters
    public String getGroupName()
    {
        return groupName;
    }
    
    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }
    
    public RequirementType getType()
    {
        return type;
    }
    
    public void setType(RequirementType type)
    {
        this.type = type;
    }
    
    public List<String> getPropertyNames()
    {
        return new ArrayList<>(propertyNames);
    }
    
    public void setPropertyNames(List<String> propertyNames)
    {
        this.propertyNames = new ArrayList<>(propertyNames);
    }
    
    public String getDescription()
    {
        return description != null ? description : type.getDescription();
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public int getPropertyCount()
    {
        return propertyNames.size();
    }
    
    @Override
    public String toString()
    {
        return String.format("RequirementGroup[name=%s, type=%s, properties=%s]", 
            groupName, type, propertyNames);
    }
}