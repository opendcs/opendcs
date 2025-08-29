package decodes.util;

import java.util.*;

/**
 * Utility class for validating property groups and requirement relationships.
 * This class provides methods to check requirement satisfaction and generate
 * validation errors for property groups.
 */
public class PropertyGroupValidator
{
    private Map<String, RequirementGroup> requirementGroups;
    private Map<String, List<RequirementGroup>> propertyToGroups;
    
    /**
     * Constructor
     * @param requirementGroups Map of requirement group names to RequirementGroup objects
     * @param propertyToGroups Map of property names to their requirement groups
     */
    public PropertyGroupValidator(Map<String, RequirementGroup> requirementGroups,
                                 Map<String, List<RequirementGroup>> propertyToGroups)
    {
        this.requirementGroups = requirementGroups != null ? 
            new HashMap<>(requirementGroups) : new HashMap<>();
        this.propertyToGroups = propertyToGroups != null ? 
            new HashMap<>(propertyToGroups) : new HashMap<>();
    }
    
    /**
     * Get all requirement groups including individually required properties
     * @return Map of requirement group names to RequirementGroup objects
     */
    public Map<String, RequirementGroup> getRequirementGroups()
    {
        return new HashMap<>(requirementGroups);
    }
    
    /**
     * Check if a property is required (either individually or as part of a group)
     * @param propertyName The name of the property to check
     * @return true if the property is in any requirement group
     */
    public boolean isPropertyRequired(String propertyName)
    {
        List<RequirementGroup> groups = propertyToGroups.get(propertyName.toUpperCase());
        return groups != null && !groups.isEmpty();
    }
    
    /**
     * Get the requirement groups for a specific property
     * @param propertyName The name of the property
     * @return List of RequirementGroup objects this property belongs to
     */
    public List<RequirementGroup> getPropertyRequirementGroups(String propertyName)
    {
        List<RequirementGroup> groups = propertyToGroups.get(propertyName.toUpperCase());
        return groups != null ? new ArrayList<>(groups) : new ArrayList<>();
    }
    
    /**
     * Check if a requirement group is satisfied based on provided property values
     * @param groupName The name of the requirement group
     * @param providedProperties Map of property names to their values (non-null values are considered provided)
     * @return true if the group's requirements are satisfied
     */
    public boolean isRequirementGroupSatisfied(String groupName, 
                                              Map<String, String> providedProperties)
    {
        RequirementGroup group = requirementGroups.get(groupName);
        if (group == null)
        {
            return true; // No such group is considered satisfied
        }
        
        // Get list of property names that have non-empty values
        List<String> providedPropertyNames = new ArrayList<>();
        for (String propertyName : group.getPropertyNames())
        {
            String value = providedProperties.get(propertyName);
            if (value != null && !value.trim().isEmpty())
            {
                providedPropertyNames.add(propertyName);
            }
        }
        
        return group.isSatisfied(providedPropertyNames);
    }
    
    /**
     * Get validation errors for all requirement groups
     * @param providedProperties Map of property names to their values
     * @return List of validation error messages
     */
    public List<String> getValidationErrors(Map<String, String> providedProperties)
    {
        List<String> errors = new ArrayList<>();
        
        // Get list of all provided property names (with non-empty values)
        List<String> providedPropertyNames = new ArrayList<>();
        for (Map.Entry<String, String> entry : providedProperties.entrySet())
        {
            if (entry.getValue() != null && !entry.getValue().trim().isEmpty())
            {
                providedPropertyNames.add(entry.getKey());
            }
        }
        
        // Check each requirement group
        for (RequirementGroup group : requirementGroups.values())
        {
            String error = group.getValidationError(providedPropertyNames);
            if (error != null)
            {
                errors.add(error);
            }
        }
        
        return errors;
    }
    
    /**
     * Get all unsatisfied requirement groups
     * @param providedProperties Map of property names to their values
     * @return List of requirement group names that are not satisfied
     */
    public List<String> getUnsatisfiedRequirementGroups(Map<String, String> providedProperties)
    {
        List<String> unsatisfied = new ArrayList<>();
        for (String groupName : requirementGroups.keySet())
        {
            if (!isRequirementGroupSatisfied(groupName, providedProperties))
            {
                unsatisfied.add(groupName);
            }
        }
        return unsatisfied;
    }
    
    /**
     * Builder class for constructing PropertyGroupValidator instances
     */
    public static class Builder
    {
        private Map<String, RequirementGroup> requirementGroups = new HashMap<>();
        private Map<String, List<RequirementGroup>> propertyToGroups = new HashMap<>();
        
        /**
         * Add a requirement group
         * @param group The RequirementGroup to add
         * @return this Builder instance for method chaining
         */
        public Builder addRequirementGroup(RequirementGroup group)
        {
            requirementGroups.put(group.getGroupName(), group);
            
            // Update property-to-groups mapping
            for (String propertyName : group.getPropertyNames())
            {
                propertyToGroups.computeIfAbsent(propertyName.toUpperCase(), 
                    k -> new ArrayList<>()).add(group);
            }
            
            return this;
        }
        
        /**
         * Add multiple requirement groups
         * @param groups Collection of RequirementGroup objects
         * @return this Builder instance for method chaining
         */
        public Builder addRequirementGroups(Collection<RequirementGroup> groups)
        {
            for (RequirementGroup group : groups)
            {
                addRequirementGroup(group);
            }
            return this;
        }
        
        /**
         * Build the PropertyGroupValidator
         * @return new PropertyGroupValidator instance
         */
        public PropertyGroupValidator build()
        {
            return new PropertyGroupValidator(requirementGroups, propertyToGroups);
        }
    }
}