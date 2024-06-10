package decodes.util;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Date;
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
}
