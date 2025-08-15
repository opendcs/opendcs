package decodes.util;

import java.util.*;

/**
 * Utility class for validating properties against their requirement groups.
 * This class helps manage complex requirement relationships between properties.
 */
public class PropertySpecValidator
{
    /**
     * Validate a set of provided properties against a collection of PropertySpecs
     * @param propertySpecs Collection of PropertySpec objects defining requirements
     * @param providedProperties Map of property names to their values
     * @return ValidationResult containing success status and any error messages
     */
    public static ValidationResult validate(Collection<PropertySpec> propertySpecs, 
                                           Map<String, String> providedProperties)
    {
        ValidationResult result = new ValidationResult();
        
        // Build a map of all requirement groups
        Map<String, RequirementGroup> allGroups = new HashMap<>();
        Map<String, List<PropertySpec>> groupToSpecs = new HashMap<>();
        
        for (PropertySpec spec : propertySpecs)
        {
            for (RequirementGroup group : spec.getRequirementGroups())
            {
                String groupName = group.getGroupName();
                
                // Merge groups with the same name
                if (allGroups.containsKey(groupName))
                {
                    RequirementGroup existingGroup = allGroups.get(groupName);
                    existingGroup.addProperty(spec.getName());
                }
                else
                {
                    RequirementGroup newGroup = new RequirementGroup(
                        group.getGroupName(),
                        group.getType(),
                        group.getDescription()
                    );
                    newGroup.addProperty(spec.getName());
                    allGroups.put(groupName, newGroup);
                }
                
                // Track which specs belong to each group
                groupToSpecs.computeIfAbsent(groupName, k -> new ArrayList<>()).add(spec);
            }
        }
        
        // Get list of provided property names
        List<String> providedNames = new ArrayList<>(providedProperties.keySet());
        
        // Validate each requirement group
        for (Map.Entry<String, RequirementGroup> entry : allGroups.entrySet())
        {
            RequirementGroup group = entry.getValue();
            
            if (!group.isSatisfied(providedNames))
            {
                String error = group.getValidationError(providedNames);
                result.addError(error);
            }
        }
        
        return result;
    }
    
    /**
     * Create requirement groups for a set of properties with a specific validation type
     * @param propertyNames List of property names to group
     * @param groupName Name of the requirement group
     * @param type Type of requirement validation
     * @return RequirementGroup object
     */
    public static RequirementGroup createGroup(List<String> propertyNames, 
                                              String groupName,
                                              RequirementGroup.RequirementType type)
    {
        RequirementGroup group = new RequirementGroup(groupName, type);
        for (String propName : propertyNames)
        {
            group.addProperty(propName);
        }
        return group;
    }
    
    /**
     * Build PropertySpecs with complex requirement relationships
     */
    public static class PropertySpecBuilder
    {
        private List<PropertySpec> specs = new ArrayList<>();
        private Map<String, RequirementGroup> groups = new HashMap<>();
        
        /**
         * Add a simple optional property
         */
        public PropertySpecBuilder addOptional(String name, String type, String description)
        {
            specs.add(new PropertySpec(name, type, description));
            return this;
        }
        
        /**
         * Add an individually required property
         */
        public PropertySpecBuilder addRequired(String name, String type, String description)
        {
            specs.add(new PropertySpec(name, type, description, true));
            return this;
        }
        
        /**
         * Add properties that form a "one of" group
         */
        public PropertySpecBuilder addOneOfGroup(String groupName, 
                                                PropertySpec... properties)
        {
            RequirementGroup group = new RequirementGroup(
                groupName, 
                RequirementGroup.RequirementType.ONE_OF
            );
            
            for (PropertySpec spec : properties)
            {
                group.addProperty(spec.getName());
                spec.addRequirementGroup(group);
                specs.add(spec);
            }
            
            groups.put(groupName, group);
            return this;
        }
        
        /**
         * Add properties that form an "all or none" group
         */
        public PropertySpecBuilder addAllOrNoneGroup(String groupName,
                                                    PropertySpec... properties)
        {
            RequirementGroup group = new RequirementGroup(
                groupName,
                RequirementGroup.RequirementType.ALL_OR_NONE
            );
            
            for (PropertySpec spec : properties)
            {
                group.addProperty(spec.getName());
                spec.addRequirementGroup(group);
                specs.add(spec);
            }
            
            groups.put(groupName, group);
            return this;
        }
        
        /**
         * Build the final list of PropertySpecs
         */
        public List<PropertySpec> build()
        {
            return new ArrayList<>(specs);
        }
    }
    
    /**
     * Result of property validation
     */
    public static class ValidationResult
    {
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        
        public void addError(String error)
        {
            valid = false;
            errors.add(error);
        }
        
        public boolean isValid()
        {
            return valid;
        }
        
        public List<String> getErrors()
        {
            return new ArrayList<>(errors);
        }
        
        public String getErrorMessage()
        {
            if (valid)
                return "";
            
            return String.join("\n", errors);
        }
    }
    
    /**
     * Example usage demonstrating different requirement scenarios
     */
    public static void main(String[] args)
    {
        // Example 1: Create specs with different requirement types
        PropertySpecBuilder builder = new PropertySpecBuilder();
        
        // Add individually required properties
        builder.addRequired("username", PropertySpec.STRING, "User name is required");
        builder.addRequired("password", PropertySpec.STRING, "Password is required");
        
        // Add a one-of group (either email OR phone must be provided)
        builder.addOneOfGroup("contact",
            new PropertySpec("email", PropertySpec.STRING, "Email address"),
            new PropertySpec("phone", PropertySpec.STRING, "Phone number")
        );
        
        // Add an all-or-none group (if latitude is provided, longitude must also be provided)
        builder.addAllOrNoneGroup("coordinates",
            new PropertySpec("latitude", PropertySpec.NUMBER, "Latitude coordinate"),
            new PropertySpec("longitude", PropertySpec.NUMBER, "Longitude coordinate")
        );
        
        // Add optional properties
        builder.addOptional("description", PropertySpec.LONGSTRING, "Optional description");
        
        List<PropertySpec> specs = builder.build();
        
        // Example 2: Validate properties
        Map<String, String> providedProps = new HashMap<>();
        providedProps.put("username", "john");
        providedProps.put("password", "secret");
        providedProps.put("email", "john@example.com");
        providedProps.put("latitude", "40.7128");
        // Missing longitude - will fail all-or-none validation
        
        ValidationResult result = validate(specs, providedProps);
        
        if (!result.isValid())
        {
            System.out.println("Validation failed:");
            System.out.println(result.getErrorMessage());
        }
    }
}