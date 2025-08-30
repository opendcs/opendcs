package org.opendcs.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.opendcs.annotations.AlgorithmRequirements;
import org.opendcs.annotations.PropertyRequirementGroup;
import org.opendcs.annotations.PropertySpec;
import org.opendcs.annotations.PropertySpecValidator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertySpecValidatorTest 
{
    // Test class using new class-level requirements API with multiple groups
    @AlgorithmRequirements(
        groups = {
            @AlgorithmRequirements.RequirementGroup(
                name = "auth",
                type = AlgorithmRequirements.RequirementType.ONE_OF,
                properties = {"username", "apiKey", "certificate"}
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "database",
                type = AlgorithmRequirements.RequirementType.ALL_REQUIRED,
                properties = {"host", "port", "database"}
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "contact",
                type = AlgorithmRequirements.RequirementType.AT_LEAST_ONE,
                properties = {"email", "phone"}
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "proxy",
                type = AlgorithmRequirements.RequirementType.ALL_OR_NONE,
                properties = {"proxyHost", "proxyPort"}
            )
        }
    )
    static class TestAlgorithm 
    {
        @PropertySpec(name = "apiUrl", required = true, description = "API endpoint URL")
        public String apiUrl;
        
        @PropertySpec(name = "username", description = "Username for authentication")
        public String username;
        
        @PropertySpec(name = "apiKey", description = "API key for authentication")
        public String apiKey;
        
        @PropertySpec(name = "certificate", description = "Certificate for authentication")
        public String certificate;
        
        @PropertySpec(name = "host", description = "Database host")
        public String host;
        
        @PropertySpec(name = "port", description = "Database port")
        public Integer port;
        
        @PropertySpec(name = "database", description = "Database name")
        public String database;
        
        @PropertySpec(name = "email", description = "Contact email")
        public String email;
        
        @PropertySpec(name = "phone", description = "Contact phone")
        public String phone;
        
        @PropertySpec(name = "proxyHost", description = "Proxy host")
        public String proxyHost;
        
        @PropertySpec(name = "proxyPort", description = "Proxy port")
        public Integer proxyPort;
        
        @PropertySpec(name = "optional", description = "Optional property")
        public String optional;
    }
    
    // Test class with simple required fields
    static class SimpleRequiredAlgorithm 
    {
        @PropertySpec(name = "requiredField1", required = true)
        public String requiredField1;
        
        @PropertySpec(name = "requiredField2", required = true)
        public String requiredField2;
        
        @PropertySpec(name = "optionalField")
        public String optionalField;
    }
    
    // Test class mixing both approaches
    @AlgorithmRequirements(
        groups = {
            @AlgorithmRequirements.RequirementGroup(
                name = "options",
                type = AlgorithmRequirements.RequirementType.AT_LEAST_ONE,
                properties = {"option1", "option2", "option3"}
            )
        }
    )
    static class MixedAlgorithm 
    {
        @PropertySpec(name = "mandatoryProp", required = true)
        public String mandatoryProp;
        
        @PropertySpec(name = "option1")
        public String option1;
        
        @PropertySpec(name = "option2")
        public String option2;
        
        @PropertySpec(name = "option3")
        public String option3;
    }
    
    // Test class with multiple groups of same type
    @AlgorithmRequirements(
        groups = {
            @AlgorithmRequirements.RequirementGroup(
                name = "primaryAuth",
                type = AlgorithmRequirements.RequirementType.ONE_OF,
                properties = {"username", "email"},
                description = "Primary authentication method"
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "secondaryAuth",
                type = AlgorithmRequirements.RequirementType.ONE_OF,
                properties = {"token", "apiKey"},
                description = "Secondary authentication method"
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "serverSettings",
                type = AlgorithmRequirements.RequirementType.ALL_REQUIRED,
                properties = {"host", "port"}
            ),
            @AlgorithmRequirements.RequirementGroup(
                name = "databaseSettings",
                type = AlgorithmRequirements.RequirementType.ALL_REQUIRED,
                properties = {"dbName", "dbUser", "dbPassword"}
            )
        }
    )
    static class MultipleGroupsAlgorithm 
    {
        @PropertySpec(name = "username")
        public String username;
        
        @PropertySpec(name = "email")
        public String email;
        
        @PropertySpec(name = "token")
        public String token;
        
        @PropertySpec(name = "apiKey")
        public String apiKey;
        
        @PropertySpec(name = "host")
        public String host;
        
        @PropertySpec(name = "port")
        public Integer port;
        
        @PropertySpec(name = "dbName")
        public String dbName;
        
        @PropertySpec(name = "dbUser")
        public String dbUser;
        
        @PropertySpec(name = "dbPassword")
        public String dbPassword;
    }
    
    private PropertySpecValidator validator;
    private Map<String, String> properties;
    
    @BeforeEach
    public void setup() 
    {
        validator = new PropertySpecValidator(TestAlgorithm.class);
        properties = new HashMap<>();
    }
    
    @Test
    public void testValidateWithAllRequirementsSatisfied() 
    {
        // Satisfy all requirements
        properties.put("apiUrl", "https://api.example.com");
        properties.put("username", "user123"); // ONE_OF auth
        properties.put("host", "localhost");   // ALL_REQUIRED database
        properties.put("port", "5432");
        properties.put("database", "mydb");
        properties.put("email", "test@example.com"); // AT_LEAST_ONE contact
        // proxy group satisfied by providing none (ALL_OR_NONE)
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    public void testValidateWithMissingRequired() 
    {
        // Missing required apiUrl
        properties.put("username", "user123");
        properties.put("host", "localhost");
        properties.put("port", "5432");
        properties.put("database", "mydb");
        properties.put("email", "test@example.com");
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        assertFalse(result.getErrors().isEmpty());
        
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.contains("apiUrl"));
    }
    
    @Test
    public void testValidateOneOfViolation() 
    {
        properties.put("apiUrl", "https://api.example.com");
        // Provide two auth methods (violates ONE_OF)
        properties.put("username", "user123");
        properties.put("apiKey", "key456");
        properties.put("host", "localhost");
        properties.put("port", "5432");
        properties.put("database", "mydb");
        properties.put("email", "test@example.com");
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.contains("auth"));
        assertTrue(errorMessage.contains("exactly one"));
    }
    
    @Test
    public void testValidateAllRequiredViolation() 
    {
        properties.put("apiUrl", "https://api.example.com");
        properties.put("username", "user123");
        // Missing some database properties (violates ALL_REQUIRED)
        properties.put("host", "localhost");
        properties.put("port", "5432");
        // missing 'database'
        properties.put("email", "test@example.com");
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.contains("database"));
        assertTrue(errorMessage.contains("all"));
    }
    
    @Test
    public void testValidateAtLeastOneViolation() 
    {
        properties.put("apiUrl", "https://api.example.com");
        properties.put("username", "user123");
        properties.put("host", "localhost");
        properties.put("port", "5432");
        properties.put("database", "mydb");
        // No contact method provided (violates AT_LEAST_ONE)
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.contains("contact"));
        assertTrue(errorMessage.contains("at least one"));
    }
    
    @Test
    public void testValidateAllOrNoneViolation() 
    {
        properties.put("apiUrl", "https://api.example.com");
        properties.put("username", "user123");
        properties.put("host", "localhost");
        properties.put("port", "5432");
        properties.put("database", "mydb");
        properties.put("email", "test@example.com");
        // Only partial proxy settings (violates ALL_OR_NONE)
        properties.put("proxyHost", "proxy.example.com");
        // missing proxyPort
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        
        String errorMessage = result.getErrorMessage();
        assertTrue(errorMessage.contains("proxy"));
        assertTrue(errorMessage.contains("all or none"));
    }
    
    @Test
    public void testIsPropertyRequired() 
    {
        // Test required property
        assertTrue(validator.isPropertyRequired("apiUrl"));
        
        // Test property in requirement group
        assertTrue(validator.isPropertyRequired("username"));
        assertTrue(validator.isPropertyRequired("host"));
        
        // Test optional property
        assertFalse(validator.isPropertyRequired("optional"));
        
        // Test case insensitivity
        assertTrue(validator.isPropertyRequired("APIURL"));
        assertTrue(validator.isPropertyRequired("USERNAME"));
    }
    
    @Test
    public void testGetGroupsForProperty() 
    {
        // Property in one group
        List<PropertyRequirementGroup> groups = validator.getGroupsForProperty("username");
        assertEquals(1, groups.size());
        assertEquals("auth", groups.get(0).getGroupName());
        
        // Property not in any group (but required)
        groups = validator.getGroupsForProperty("apiUrl");
        assertEquals(1, groups.size());
        assertTrue(groups.get(0).getGroupName().startsWith("_required_"));
        
        // Optional property
        groups = validator.getGroupsForProperty("optional");
        assertTrue(groups.isEmpty());
        
        // Test case insensitivity
        groups = validator.getGroupsForProperty("USERNAME");
        assertEquals(1, groups.size());
    }
    
    @Test
    public void testIsMissingPropertyViolatingRequirements() 
    {
        // Set up some valid properties
        properties.put("apiUrl", "https://api.example.com");
        properties.put("username", "user123");
        properties.put("host", "localhost");
        properties.put("port", "5432");
        properties.put("database", "mydb");
        properties.put("email", "test@example.com");
        
        // Test missing required property - should violate
        assertTrue(validator.isMissingPropertyViolatingRequirements("apiUrl", new HashMap<>()));
        
        // Test property with value - should not violate
        assertFalse(validator.isMissingPropertyViolatingRequirements("apiUrl", properties));
        
        // Test missing property in satisfied ONE_OF group - should not violate
        assertFalse(validator.isMissingPropertyViolatingRequirements("apiKey", properties));
        
        // Test missing property in unsatisfied ALL_REQUIRED group - should violate
        Map<String, String> partialProps = new HashMap<>();
        partialProps.put("host", "localhost");
        // missing port and database
        assertTrue(validator.isMissingPropertyViolatingRequirements("port", partialProps));
        
        // Test optional property - should not violate
        assertFalse(validator.isMissingPropertyViolatingRequirements("optional", properties));
    }
    
    @Test
    public void testStaticValidateClass() 
    {
        Map<String, String> props = new HashMap<>();
        props.put("apiUrl", "https://api.example.com");
        props.put("apiKey", "key123");
        props.put("host", "localhost");
        props.put("port", "5432");
        props.put("database", "mydb");
        props.put("phone", "555-1234");
        
        PropertySpecValidator.ValidationResult result = 
            PropertySpecValidator.validateClass(TestAlgorithm.class, props);
        
        assertTrue(result.isValid());
    }
    
    @Test
    public void testMultipleErrors() 
    {
        // Create a scenario with multiple violations
        properties.put("username", "user");
        properties.put("apiKey", "key"); // ONE_OF violation
        properties.put("host", "localhost"); // Missing port and database
        // Missing contact
        // Missing apiUrl
        
        PropertySpecValidator.ValidationResult result = validator.validate(properties);
        assertFalse(result.isValid());
        
        List<PropertySpecValidator.ValidationError> errors = result.getErrors();
        assertTrue(errors.size() >= 3); // At least 3 violations
        
        String errorMessage = result.getErrorMessage();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.length() > 0);
    }
    
    @Test
    public void testSimpleRequiredFields() 
    {
        PropertySpecValidator simpleValidator = new PropertySpecValidator(SimpleRequiredAlgorithm.class);
        
        Map<String, String> props = new HashMap<>();
        PropertySpecValidator.ValidationResult result = simpleValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() == 2);
        
        props.put("requiredField1", "value1");
        result = simpleValidator.validate(props);
        assertFalse(result.isValid());
        
        props.put("requiredField2", "value2");
        result = simpleValidator.validate(props);
        assertTrue(result.isValid());
        
        props.put("optionalField", "optional");
        result = simpleValidator.validate(props);
        assertTrue(result.isValid());
    }
    
    @Test
    public void testMixedRequirements() 
    {
        PropertySpecValidator mixedValidator = new PropertySpecValidator(MixedAlgorithm.class);
        
        Map<String, String> props = new HashMap<>();
        PropertySpecValidator.ValidationResult result = mixedValidator.validate(props);
        assertFalse(result.isValid());
        
        // Add mandatory property
        props.put("mandatoryProp", "required");
        result = mixedValidator.validate(props);
        assertFalse(result.isValid());
        
        // Add one option
        props.put("option1", "opt1");
        result = mixedValidator.validate(props);
        assertTrue(result.isValid());
        
        // Add more options (should still pass)
        props.put("option2", "opt2");
        result = mixedValidator.validate(props);
        assertTrue(result.isValid());
    }
    
    @Test
    public void testClassLevelRequirementsHighlighting() 
    {
        PropertySpecValidator validator = new PropertySpecValidator(TestAlgorithm.class);
        
        Map<String, String> props = new HashMap<>();
        
        // When no properties are set, username/apiKey/certificate violate oneOf requirement
        assertTrue(validator.isMissingPropertyViolatingRequirements("username", props));
        assertTrue(validator.isMissingPropertyViolatingRequirements("apiKey", props));
        
        // proxyHost and proxyPort don't violate allOrNone when both are missing
        assertFalse(validator.isMissingPropertyViolatingRequirements("proxyHost", props));
        assertFalse(validator.isMissingPropertyViolatingRequirements("proxyPort", props));
        
        // Add username - now apiKey doesn't violate
        props.put("username", "value");
        assertFalse(validator.isMissingPropertyViolatingRequirements("apiKey", props));
        
        // Add proxyHost - now proxyPort violates allOrNone
        props.put("proxyHost", "proxy");
        assertTrue(validator.isMissingPropertyViolatingRequirements("proxyPort", props));
    }
    
    @Test
    public void testSimpleRequiredHighlighting() 
    {
        PropertySpecValidator validator = new PropertySpecValidator(SimpleRequiredAlgorithm.class);
        
        Map<String, String> props = new HashMap<>();
        
        // Both required fields should be highlighted when missing
        assertTrue(validator.isMissingPropertyViolatingRequirements("requiredField1", props));
        assertTrue(validator.isMissingPropertyViolatingRequirements("requiredField2", props));
        
        // Optional field should not be highlighted
        assertFalse(validator.isMissingPropertyViolatingRequirements("optionalField", props));
        
        // Add requiredField1 - it's no longer violating
        props.put("requiredField1", "value1");
        assertFalse(validator.isMissingPropertyViolatingRequirements("requiredField1", props));
        assertTrue(validator.isMissingPropertyViolatingRequirements("requiredField2", props));
    }
    
    @Test
    public void testMultipleGroupsOfSameType() 
    {
        PropertySpecValidator multiValidator = new PropertySpecValidator(MultipleGroupsAlgorithm.class);
        
        Map<String, String> props = new HashMap<>();
        
        // Test both ONE_OF groups must be satisfied
        props.put("username", "user"); // primaryAuth ONE_OF
        props.put("token", "tok123");  // secondaryAuth ONE_OF
        props.put("host", "localhost"); // serverSettings ALL_REQUIRED
        props.put("port", "8080");
        props.put("dbName", "testdb"); // databaseSettings ALL_REQUIRED
        props.put("dbUser", "dbuser");
        props.put("dbPassword", "pass");
        
        PropertySpecValidator.ValidationResult result = multiValidator.validate(props);
        assertTrue(result.isValid());
        
        // Test violation of first ONE_OF group
        props.put("email", "test@example.com"); // Violates primaryAuth ONE_OF
        result = multiValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("primaryAuth"));
        
        props.remove("email");
        
        // Test violation of second ONE_OF group
        props.put("apiKey", "key456"); // Violates secondaryAuth ONE_OF
        result = multiValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("secondaryAuth"));
        
        props.remove("apiKey");
        
        // Test missing from first ALL_REQUIRED group
        props.remove("port");
        result = multiValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("serverSettings"));
        
        props.put("port", "8080");
        
        // Test missing from second ALL_REQUIRED group
        props.remove("dbPassword");
        result = multiValidator.validate(props);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("databaseSettings"));
    }
}