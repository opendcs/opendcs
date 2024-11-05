/*
*  $Id$
*/
package ilex.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Enumeration;

import decodes.util.PropertySpec;

/**
A collection of static utility methods for manipulating java.util.Properties
sets.
*/
public class PropertiesUtil
{
	public static final String possibleDelims  = ",;|#+!~^&*";
	public static final String possibleAssigns = "=:><`()[]";
	
	/**
	* Returns true if two properties sets are the same. Meaning they have
	* exactly the same keys and values.
	* In this method, keys are compared case SENSITIVE.
	* @param p1 the first properties set.
	* @param p2 the second properties set.
	* @return true if keys & values in both sets are equal, or if both are null.
	*/
	public static boolean propertiesEqual( Properties p1, Properties p2 )
	{
		if (p1 == null)
			return p2 == null;
		else if (p2 == null)
			return false;
		
		if (p1.size() != p2.size())
		{
Logger.instance().debug3("differing num of props p1='" + props2string(p1) + "' p2='" + props2string(p2) + "'");
			return false;
		}
		for(Enumeration it = p1.keys(); it.hasMoreElements();)
		{
			Object k = it.nextElement();
			Object v1 = p1.get(k);
			Object v2 = p2.get(k);
			if (v2 == null || !v1.equals(v2))
			{
Logger.instance().debug3("Property " + k + " values differ '" + v1 + "' '" + v2 + "'");
				return false;
			}
		}
		return true;
	}

	/**
	* Returns the properties in a comma-separated string.
	* Warning: no accomodation is made for property values that may contain
	* commas.
	* @param pr the properties set
	* @return String representation
	*/
	public static String props2string( Properties pr )
	{
		int dIdx = 0, aIdx = 0;
	  nextDelim:
		while(dIdx < possibleDelims.length())
		{
			for(Object v : pr.values())
			{
				String sv = (String)v;
				if (sv.indexOf(possibleDelims.charAt(dIdx)) >= 0)
				{
					dIdx++;
					continue nextDelim;
				}
			}
			break;
		}
	  nextAssign:
		while(aIdx < possibleAssigns.length())
		{
			for(Object v : pr.values())
			{
				String sv = (String)v;
				if (sv.indexOf(possibleAssigns.charAt(aIdx)) >= 0)
				{
					aIdx++;
					continue nextAssign;
				}
			}
			break;
		}

		if (dIdx == possibleDelims.length())
		{
			Logger.instance().warning("Cannot encode props because values contain "
				+ "all possible delims " + possibleDelims);
			dIdx = aIdx = 0;
		}
		if (aIdx == possibleAssigns.length())
		{
			Logger.instance().warning("Cannot encode props because values contain"
				+ " all possible assignment operators " + possibleAssigns);
			dIdx = aIdx = 0;
		}
		StringBuffer ret = new StringBuffer();
		char delim = possibleDelims.charAt(dIdx);
		char assign = possibleAssigns.charAt(aIdx);
		if (dIdx != 0 || aIdx != 0)
		{
			ret.append(delim);
			ret.append(assign);
		}
		int i=0;
		Enumeration en = pr.propertyNames();
		while(en.hasMoreElements())
		{
			String key = (String)en.nextElement();
			String value = pr.getProperty(key);
			if (i++ > 0)
			{
				ret.append(delim);
				ret.append(' ');
			}
			ret.append(key);
			ret.append(assign);
			ret.append(value);
		}
		return ret.toString();
	}

	/**
	* Converts string containing comma-separated assignments into a Properties
	* set.
	* @param s the string
	* @return the Properties
	*/
	public static Properties string2props( String s )
	{
		Properties ret = new Properties();
		if (s == null)
			return ret;
		s = s.trim();
		if (s.length() < 3) // Would have to have at least one name=value
			return ret;
		
		char delim = ',';
		char assign = '=';
		if (!Character.isLetterOrDigit(s.charAt(0)))
		{
			delim = s.charAt(0);
			assign = s.charAt(1);
			s = s.substring(2);
		}

		StringTokenizer tokenizer = new StringTokenizer(s, "" + delim);
//			TextUtil.collapseWhitespace(s), ",");
	
		while(tokenizer.hasMoreTokens())
		{
			String tok = tokenizer.nextToken().trim();
			int ei = tok.indexOf(assign);
			if (ei == -1)
				ret.setProperty(tok, "");
			else
				ret.setProperty(tok.substring(0,ei).trim(), 
					tok.substring(ei+1).trim());
		}
		return ret;
	}

	/**
	* Search for a property, ignoring case
	* @param pr the Properties
	* @param key the key
	* @return the value, or null if no match
	*/
	public static String getIgnoreCase( Properties pr, String key )
	{
		return getIgnoreCase(pr, key, null);
	}

	/**
	* Search for a property, ignoring case
	* @param pr the Properties
	* @param key the key
	* @return the value, or null if no match
	*/
	public static String getIgnoreCase( Properties pr, String key, String defaultValue)
	{
		Enumeration en = pr.propertyNames();		
		while(en.hasMoreElements())
		{
			String k = (String)en.nextElement();			
			if (key.trim().equalsIgnoreCase(k.trim()))
				return pr.getProperty(k);
		}
		return defaultValue;
	}

	/**
	* Search for a property, remove it and return its value, ignoring case in 
	* the name.
	* @param pr the Properties
	* @param key the key
	* @return the property value if found, null if not found.
	*/
	public static String rmIgnoreCase(Properties pr, String key)
	{
		Enumeration en = pr.propertyNames();
		while(en.hasMoreElements())
		{
			String k = (String)en.nextElement();
			if (key.equalsIgnoreCase(k))
			{
				String ret = pr.getProperty(k);
				pr.remove(k);
				return ret;
			}
		}
		return null;
	}

	/**
	 * Load object from properties set.
	 * @see loadFromProps(Object, Properties, String[])
	 */
	public static void loadFromProps( Object obj, Properties props)
	{
		loadFromProps(obj, props, null);
	}

	/**
	* Loads an object from a properties set.
	* Looks for public attributes in the passed object that match names in
	* the propertes set.
	* Issues log warning message for properties that don't match.
	* @param obj object containing public attributes
	* @param props the Properties set to load the object from
	* @param ignorePfx Ignore any property that starts with these prefixes,
	*  without generating a warning.
	*/
	public static void loadFromProps( Object obj, Properties props,
		String ignorePfx[])
	{
		// Use reflection to get declared public fields to match property names.
		Class cls = obj.getClass();
		Properties otherProps = null;
		try 
		{
			Field opf = cls.getField("otherProps");
			otherProps = (Properties)opf.get(obj); 
		}
		catch(Throwable ex)
		{
			otherProps = null;
		}

		Field[] fields = cls.getDeclaredFields();
		
		Enumeration names = props.propertyNames();
	  nextName:
		while(names.hasMoreElements())
		{
			String pname = (String)names.nextElement();
			String pval = props.getProperty(pname);

			int fidx;
			for(fidx=0; fidx<fields.length; fidx++)
			{
				// Only look at non-final public fields.
				Field fld = fields[fidx];
				int mods = fld.getModifiers();
				if (!Modifier.isPublic(mods) 
				 || Modifier.isFinal(mods)
				 || Modifier.isStatic(mods))
					continue;

				String fname = fld.getName();
				if (fname.equalsIgnoreCase(pname))
				{
					try
					{
						String ftyp = fld.getType().getName();
						if (ftyp.equals("java.lang.String"))
						{
							fld.set(obj, pval);
						}
						else if (ftyp.equals("char"))
						{
							fld.setChar(obj, pval.charAt(0));
						}
						else if (ftyp.equals("int"))
						{
							fld.setInt(obj, Integer.parseInt(pval));
						}
						else if (ftyp.equals("long"))
							fld.setLong(obj, Long.parseLong(pval));
						else if (ftyp.equals("float"))
							fld.setFloat(obj, Float.parseFloat(pval));
						else if (ftyp.equals("double"))
							fld.setDouble(obj, Double.parseDouble(pval));
						else if (ftyp.equals("boolean"))
							fld.setBoolean(obj, TextUtil.str2boolean(pval));
						else
						{													
							Logger.instance().warning(
								"Unsupported field type '" + ftyp 
								+ "' for field '" + fname + "' - skipped.");
						}
					}
					catch(NumberFormatException ex)
					{
						Logger.instance().warning(
							"Field '" + fname 
							+ "' requires a number. Illegal value '"
							+ pval + "' skipped.");
					}
					catch(IllegalAccessException ex)
					{
						Logger.instance().warning(
							"Internal problem processing field '" + pname 
							+ "' - skipped: " + ex);
					}
					break;
				}
			}
			if (fidx == fields.length) // fell through)
			{
				if (ignorePfx != null)
					for(int i=0; i<ignorePfx.length; i++)
						if (TextUtil.startsWithIgnoreCase(pname, ignorePfx[i]))
							continue nextName;
				if (otherProps != null)
					otherProps.setProperty(pname, pval);
				Logger.instance().debug1("Property '" + pname 
					+ "' does not match public attribute in "
					+ cls.getName());
			}
		}
	}

	/**
	* Saves public attributes in an object to a properties set.
	* @param obj Object containing public attributes
	* @param props Properties set in which to store values.
	* @param prefix If not null, this is placed before each property name.
	*/
	public static void storeInProps(Object obj, Properties props, String prefix)
	{
		// Use reflection to get declared public fields to match property names.
		Class cls = obj.getClass();

		try 
		{
			Field opf = cls.getField("otherProps");
			Properties otherProps = (Properties)opf.get(obj); 
			Enumeration penum = otherProps.propertyNames();
			while(penum.hasMoreElements())
			{
				String nm = (String)penum.nextElement();
				String v = otherProps.getProperty(nm);
				props.setProperty(nm, v);
			}
		}
		catch(Throwable ex) { }

		Field[] fields = cls.getDeclaredFields();

		for(int fidx=0; fidx<fields.length; fidx++)
		{
			// Only look at public fields.
			Field fld = fields[fidx];
			int mods = fld.getModifiers();
			if (!Modifier.isPublic(mods) 
			 || Modifier.isFinal(mods)
			 || Modifier.isStatic(mods))
				continue;

			String fname = fld.getName();
			String pname = prefix != null ? (prefix+fname) : fname;
			String ftyp = fld.getType().getName();
			try
			{
				if (ftyp.equals("java.lang.String"))
				{
					String value = (String)fld.get(obj);
					if (value != null)
						props.setProperty(pname, value);
				}
				else if (ftyp.equals("char"))
				{
					char c = fld.getChar(obj);
					if (c != '\0')
						props.setProperty(pname, "" + c);
				}
				else if (ftyp.equals("int"))
				{
					int i = fld.getInt(obj);
					if (i != Integer.MIN_VALUE)
						props.setProperty(pname, "" + i);
				}
				else if (ftyp.equals("long"))
				{
					long l = fld.getLong(obj);
					if (l != Long.MIN_VALUE)
						props.setProperty(pname, "" + l);
				}
				else if (ftyp.equals("float"))
				{
					float f = fld.getFloat(obj);
					if (f != Float.MIN_VALUE)
						props.setProperty(pname, "" + f);
				}
				else if (ftyp.equals("double"))
				{
					double d = fld.getDouble(obj);
					if (d != Double.MIN_VALUE
					 && d != Double.MAX_VALUE
					 && d != Double.NEGATIVE_INFINITY)
						props.setProperty(pname, "" + d);
				}
				else if (ftyp.equals("boolean"))
					props.setProperty(pname, "" + fld.getBoolean(obj));
				
				//kludge added by Shweta to get rid of warning errors. needs to be correctly implemented
				else if(ftyp.equals("decodes.sql.DbKey")){}
				else if (!pname.equals("otherProps"))
				{					
					Logger.instance().warning(
						"Unsupported field type '" + ftyp 
						+ "' for field '" + fname + "' - skipped.");
				}
			}
			catch(IllegalAccessException ex)
			{
				Logger.instance().warning(
					"IllegalAccessException in field '" + fname
					+ "' - skipped.");
			}
		}
	}

	/**
	 * Copies all top-level values from one properties set into another.
	 * Does not change the 'default' properties of the target.
	 * The targetProps is not cleared first, so after return it will have
	 * all its original properties plus the added/overwritten ones from
	 * the sourceProps.
	 * @param targetProps the properties set to copy to.
	 * @param sourceProps the properties set to copy from.
	 */
	public static void copyProps(Properties targetProps, Properties sourceProps)
	{
		for(Enumeration e = sourceProps.propertyNames(); e.hasMoreElements(); )
		{
			String k = (String)e.nextElement();
			targetProps.setProperty(k, sourceProps.getProperty(k));
		}
	}

	/**
	 * Combine two lists of PropertySpecs and return the combined list.
	 * This is frequently used in hierarchies where a sub-class adds its
	 * specs to the base class.
	 * @param specs1
	 * @param specs2
	 * @return combined list.
	 */
	public static PropertySpec[]
		combineSpecs(PropertySpec[] specs1, PropertySpec[] specs2)
	{
		PropertySpec[] ret = new PropertySpec[specs1.length + specs2.length];
		for(int i=0; i<specs1.length; i++)
			ret[i] = specs1[i];
		for(int i=0; i<specs2.length; i++)
			ret[i+specs1.length] = specs2[i];
		return ret;
	}
	
	public static void main(String args[])
	{
		System.out.println("Enter properties encoded as a string. Blank line when done.");
		String line;
		while((line = System.console().readLine()) != null)
		{
			Properties p = string2props(line);
			for(Object key : p.keySet())
			{
				String keys = (String)key;
				String val = p.getProperty(keys);
				System.out.println(keys + " = " + val);
			}
			System.out.println("Re-encoded as string: ");
			System.out.println(props2string(p));
			System.out.println();
		}
	}

}
