package decodes.util;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Used by the PropertiesOwner interface, a PropertySpec describes a property
 * supported by some class including its name, type, and description.
 * 
 * @author mmaloney, Mike Maloney, Cove Software LLC
 */
public class PropertySpec
	implements Serializable
{
	/** The name of the property in a Properties object */
	private String name = null;
	
	/**
	 * A coded string describing the type of the property (see constant prop types herein).
	 */
	private String type = null;
	
	/** A description of this property */
	private String description = null;

    /** true if this property is required, false if it is optional */
    private boolean required = false;
    
    /** 
     * The requirement groups this property belongs to.
     * A property can belong to multiple requirement groups with different validation types.
     * Empty list means the property is optional.
     */
    private List<RequirementGroup> requirementGroups = new ArrayList<>();

	/** type for a long integer property */
	public static final String INT = "i";

	/** type for a double precision floating point number property */
	public static final String NUMBER = "n";

	/** Type for a boolean property */
	public static final String BOOLEAN = "b";
	
	/** Type for a filename property -- must be properly formatted for underlying OS */
	public static final String FILENAME = "f";
	
	/** Type for a directory name property -- must be properly formatted for underlying OS */
	public static final String DIRECTORY = "d";

	/** Type for a free-form string property */
	public static final String STRING = "s";
	
	/** property representing one of Java's supported timezones */
	public static final String TIMEZONE = "t";

	/** A DECODES enum property type will be e:<enumname> */
	public static final String DECODES_ENUM = "e:";

	/** A hostname or IP address */
	public static final String HOSTNAME = "h";

	/** A Java enum property type will be E:<fullEnumClassPath> */
	public static final String JAVA_ENUM = "E:";
	
	/** A long string property that should be displayed in a multi-line TextArea */
	public static final String LONGSTRING = "l";

	public static final String COLOR = "color";
	
	private boolean dynamic = false;

    public PropertySpec(String name, String type, String description)
    {
        super();
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = false;
        this.requirementGroups = new ArrayList<>();
    }
	
	public PropertySpec(String name, String type, String description, boolean required)
	{
		super();
		this.name = name;
		this.type = type;
		this.description = description;
        this.required = required;
        this.requirementGroups = new ArrayList<>();
        // If required is true, create an individual requirement group
        if (required)
        {
            RequirementGroup group = new RequirementGroup(
                "_required_" + name, 
                RequirementGroup.RequirementType.INDIVIDUAL,
                "Property '" + name + "' is required"
            );
            group.addProperty(name);
            this.requirementGroups.add(group);
        }
	}
	
	public PropertySpec(String name, String type, String description, boolean required, String requirementGroupName)
	{
		super();
		this.name = name;
		this.type = type;
		this.description = description;
        this.requirementGroups = new ArrayList<>();
        
        if (requirementGroupName != null && !requirementGroupName.isEmpty())
        {
            // Create a ONE_OF requirement group (default for named groups)
            RequirementGroup group = new RequirementGroup(
                requirementGroupName,
                RequirementGroup.RequirementType.ONE_OF
            );
            group.addProperty(name);
            this.requirementGroups.add(group);
            this.required = true;
        }
        else if (required)
        {
            // Create an individual requirement group
            RequirementGroup group = new RequirementGroup(
                "_required_" + name, 
                RequirementGroup.RequirementType.INDIVIDUAL,
                "Property '" + name + "' is required"
            );
            group.addProperty(name);
            this.requirementGroups.add(group);
            this.required = true;
        }
        else
        {
            this.required = false;
        }
	}
	
	/**
	 * Constructor with requirement groups
	 * @param name Property name
	 * @param type Property type
	 * @param description Property description
	 * @param requirementGroups List of requirement groups this property belongs to
	 */
	public PropertySpec(String name, String type, String description, List<RequirementGroup> requirementGroups)
	{
		super();
		this.name = name;
		this.type = type;
		this.description = description;
		this.requirementGroups = requirementGroups != null ? new ArrayList<>(requirementGroups) : new ArrayList<>();
		this.required = !this.requirementGroups.isEmpty();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

    public boolean isRequired() { return required; }

    public void setRequired(boolean required) 
    { 
        this.required = required;
        // When setting required to true, create an individual requirement group if not already in one
        if (required && requirementGroups.isEmpty()) 
        {
            RequirementGroup group = new RequirementGroup(
                "_required_" + name, 
                RequirementGroup.RequirementType.INDIVIDUAL,
                "Property '" + name + "' is required"
            );
            group.addProperty(name);
            this.requirementGroups.add(group);
        } 
        else if (!required) 
        {
            // Remove any individual requirement groups
            requirementGroups.removeIf(g -> 
                g.getGroupName().startsWith("_required_") && 
                g.getType() == RequirementGroup.RequirementType.INDIVIDUAL
            );
        }
    }
    
    /**
     * Get the list of requirement groups this property belongs to
     * @return List of RequirementGroup objects
     */
    public List<RequirementGroup> getRequirementGroups() 
    { 
        return new ArrayList<>(requirementGroups); 
    }
    
    /**
     * Set the requirement groups for this property
     * @param groups List of RequirementGroup objects
     */
    public void setRequirementGroups(List<RequirementGroup> groups) 
    { 
        this.requirementGroups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
        this.required = !this.requirementGroups.isEmpty();
    }
    
    /**
     * Add a requirement group to this property
     * @param group The RequirementGroup to add
     */
    public void addRequirementGroup(RequirementGroup group)
    {
        if (group != null && !requirementGroups.contains(group))
        {
            requirementGroups.add(group);
            this.required = true;
        }
    }
    
    /**
     * Remove a requirement group from this property
     * @param groupName The name of the group to remove
     */
    public void removeRequirementGroup(String groupName)
    {
        requirementGroups.removeIf(g -> g.getGroupName().equals(groupName));
        this.required = !requirementGroups.isEmpty();
    }
    
    /**
     * Check if this property is individually required (not part of a mutual exclusion group)
     * @return true if this property has an individual requirement group
     */
    public boolean isIndividuallyRequired() 
    {
        return requirementGroups.stream()
            .anyMatch(g -> g.getType() == RequirementGroup.RequirementType.INDIVIDUAL);
    }
    
    /**
     * Check if this property is part of a mutual exclusion requirement group
     * @return true if this property is part of a ONE_OF group
     */
    public boolean isPartOfMutualExclusionGroup() 
    {
        return requirementGroups.stream()
            .anyMatch(g -> g.getType() == RequirementGroup.RequirementType.ONE_OF);
    }
    
    /**
     * Check if this property is part of an all-or-none requirement group
     * @return true if this property is part of an ALL_OR_NONE group
     */
    public boolean isPartOfAllOrNoneGroup() 
    {
        return requirementGroups.stream()
            .anyMatch(g -> g.getType() == RequirementGroup.RequirementType.ALL_OR_NONE);
    }
    
    /**
     * Get a simple string representation of requirement groups for backwards compatibility
     * @return Comma-separated list of group names, or empty string if none
     */
    public String getRequirementGroupNames()
    {
        if (requirementGroups.isEmpty())
            return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < requirementGroups.size(); i++)
        {
            if (i > 0) sb.append(",");
            sb.append(requirementGroups.get(i).getGroupName());
        }
        return sb.toString();
    }
	
	public String toString()
	{
		return name + ":" + type + ":" + description;
	}

	public boolean isDynamic()
	{
		return dynamic;
	}

	public void setDynamic(boolean dynamic)
	{
		this.dynamic = dynamic;
	}

	/**
	 * Helper function for processing new annotations to existing property spec until
	 * full transition to annotation can be made
	 * @param annotation Spec annotation
	 * @param field Field to which the annotation was attached
	 * @return The appropriate decodes.util.PropertySpec type for the annotation and field.
	 */
	public static String getSpecTypeFromAnnotation(org.opendcs.annotations.PropertySpec annotation, Field field)
	{
		String specType = annotation.propertySpecType();
		
		Class<?> fieldType = field.getType();
		if (!specType.isEmpty())
		{
			return specType;
		}
		
		if (fieldType == Boolean.class)
		{
			specType = PropertySpec.BOOLEAN;
		}
		else if (fieldType == Integer.class || fieldType == Long.class)
		{
			specType = PropertySpec.INT;
		}
		else if (fieldType == Float.class || fieldType == Double.class)
		{
			specType = PropertySpec.NUMBER;
		}
		else if (fieldType == Date.class)
		{
			specType = PropertySpec.STRING;
		}
		else if (fieldType == File.class)
		{
			specType = PropertySpec.FILENAME;
		}
		else if (fieldType == Path.class)
		{
			specType = PropertySpec.DIRECTORY;
		}
		else if (fieldType == Color.class)
		{
			specType = PropertySpec.COLOR;
		}
		else if (fieldType.isEnum())
		{
			specType = PropertySpec.JAVA_ENUM + fieldType.getName();
		}
		else if (fieldType == TimeZone.class)
		{
			specType = PropertySpec.TIMEZONE;
		}
		else if (fieldType == InetAddress.class || InetAddress.class.isAssignableFrom(fieldType))
		{
			specType = PropertySpec.HOSTNAME;
		}
		else
		{
			specType = PropertySpec.STRING;
		}
		return specType;
	}

	/**
	 * Determine appropriate name for this property. As defined or the field name if not.
	 * @param field
	 * @param spec
	 * @return
	 */
	public static String getPropertyName(Field field, org.opendcs.annotations.PropertySpec spec)
    {
        String name = spec.name();
        if (name.isEmpty())
        {
            name = field.getName();
        }
        return name;
    }
}
