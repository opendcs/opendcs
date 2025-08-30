package org.opendcs.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.opendcs.annotations.PropertyRequirementGroup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class PropertyRequirementGroupTest 
{
    private Map<String, String> properties;
    
    @BeforeEach
    public void setup() 
    {
        properties = new HashMap<>();
    }
    
    @Test
    public void testOneOfRequirement() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "auth", 
            PropertyRequirementGroup.GroupType.ONE_OF,
            "Authentication method"
        );
        group.addProperty("username");
        group.addProperty("apiKey");
        group.addProperty("certificate");
        
        // Test no properties provided - should fail
        assertFalse(group.isSatisfied(properties));
        
        // Test exactly one property - should pass
        properties.put("username", "user123");
        assertTrue(group.isSatisfied(properties));
        
        // Test two properties - should fail
        properties.put("apiKey", "key456");
        assertFalse(group.isSatisfied(properties));
        
        // Test all properties - should fail
        properties.put("certificate", "cert789");
        assertFalse(group.isSatisfied(properties));
    }
    
    @Test
    public void testAllOrNoneRequirement() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "credentials",
            PropertyRequirementGroup.GroupType.ALL_OR_NONE,
            "Complete credentials or none"
        );
        group.addProperty("username");
        group.addProperty("password");
        
        // Test no properties - should pass
        assertTrue(group.isSatisfied(properties));
        
        // Test one property only - should fail
        properties.put("username", "user");
        assertFalse(group.isSatisfied(properties));
        
        // Test both properties - should pass
        properties.put("password", "pass");
        assertTrue(group.isSatisfied(properties));
    }
    
    @Test
    public void testAtLeastOneRequirement() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "contact",
            PropertyRequirementGroup.GroupType.AT_LEAST_ONE,
            "At least one contact method"
        );
        group.addProperty("email");
        group.addProperty("phone");
        group.addProperty("address");
        
        // Test no properties - should fail
        assertFalse(group.isSatisfied(properties));
        
        // Test one property - should pass
        properties.put("email", "test@example.com");
        assertTrue(group.isSatisfied(properties));
        
        // Test two properties - should pass
        properties.put("phone", "555-1234");
        assertTrue(group.isSatisfied(properties));
        
        // Test all properties - should pass
        properties.put("address", "123 Main St");
        assertTrue(group.isSatisfied(properties));
    }
    
    @Test
    public void testAllRequiredRequirement() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "database",
            PropertyRequirementGroup.GroupType.ALL_REQUIRED,
            "All database settings required"
        );
        group.addProperty("host");
        group.addProperty("port");
        group.addProperty("database");
        
        // Test no properties - should fail
        assertFalse(group.isSatisfied(properties));
        
        // Test one property - should fail
        properties.put("host", "localhost");
        assertFalse(group.isSatisfied(properties));
        
        // Test two properties - should fail
        properties.put("port", "5432");
        assertFalse(group.isSatisfied(properties));
        
        // Test all properties - should pass
        properties.put("database", "mydb");
        assertTrue(group.isSatisfied(properties));
    }
    
    @Test
    public void testIndividualRequirement() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "_required_apiUrl",
            PropertyRequirementGroup.GroupType.INDIVIDUAL,
            "API URL is required"
        );
        group.addProperty("apiUrl");
        
        // Test no value - should fail
        assertFalse(group.isSatisfied(properties));
        
        // Test with value - should pass
        properties.put("apiUrl", "https://api.example.com");
        assertTrue(group.isSatisfied(properties));
    }
    
    @Test
    public void testEmptyValueTreatedAsMissing() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "required",
            PropertyRequirementGroup.GroupType.INDIVIDUAL
        );
        group.addProperty("field");
        
        // Empty string should be treated as missing
        properties.put("field", "");
        assertFalse(group.isSatisfied(properties));
        
        // Whitespace only should be treated as missing
        properties.put("field", "   ");
        assertFalse(group.isSatisfied(properties));
        
        // Actual value should pass
        properties.put("field", "value");
        assertTrue(group.isSatisfied(properties));
    }
    
    @Test
    public void testValidationErrorMessages() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "auth",
            PropertyRequirementGroup.GroupType.ONE_OF
        );
        group.addProperty("username");
        group.addProperty("apiKey");
        
        // Test error message when no properties provided
        String error = group.getValidationError(properties);
        assertNotNull(error);
        assertTrue(error.contains("auth"));
        assertTrue(error.contains("exactly one"));
        
        // Test no error when requirement satisfied
        properties.put("username", "user");
        assertNull(group.getValidationError(properties));
        
        // Test error message when too many properties provided
        properties.put("apiKey", "key");
        error = group.getValidationError(properties);
        assertNotNull(error);
        assertTrue(error.contains("exactly one"));
        assertTrue(error.contains("found 2"));
    }
    
    @Test
    public void testGetDescription() 
    {
        // Test custom description
        PropertyRequirementGroup group1 = new PropertyRequirementGroup(
            "test",
            PropertyRequirementGroup.GroupType.ONE_OF,
            "Custom description"
        );
        assertEquals("Custom description", group1.getDescription());
        
        // Test default descriptions for each type
        PropertyRequirementGroup group2 = new PropertyRequirementGroup(
            "test",
            PropertyRequirementGroup.GroupType.ONE_OF
        );
        assertTrue(group2.getDescription().contains("Exactly one"));
        
        PropertyRequirementGroup group3 = new PropertyRequirementGroup(
            "test",
            PropertyRequirementGroup.GroupType.ALL_OR_NONE
        );
        assertTrue(group3.getDescription().contains("all"));
        assertTrue(group3.getDescription().contains("none"));
    }
    
    @Test
    public void testAddPropertyNoDuplicates() 
    {
        PropertyRequirementGroup group = new PropertyRequirementGroup(
            "test",
            PropertyRequirementGroup.GroupType.ONE_OF
        );
        
        group.addProperty("prop1");
        group.addProperty("prop1"); // Try to add duplicate
        group.addProperty("prop2");
        
        assertEquals(2, group.getPropertyNames().size());
        assertTrue(group.getPropertyNames().contains("prop1"));
        assertTrue(group.getPropertyNames().contains("prop2"));
    }
}