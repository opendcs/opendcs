package ilex.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utilities for dealing with java beans.
 * @author mmaloney
 *
 */
public class BeanUtil
{
	public static SimpleDateFormat sdf = new SimpleDateFormat(
		"yyyy/MM/dd-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	/**
	 * Calls a bean's setXXX method through reflection. Intended for setting
	 * bean attributes from a properties file where you have property names
	 * and string values.
	 * <p>
	 * Look for a matching 'set' method for the passed name. Examine the method's
	 * argument type and convert the value if necessary before calling the set 
	 * method.
	 * @param bean the bean object
	 * @param name the attribute name (non-case sensitive)
	 * @param value the attribute value as a string
	 * @return true if matching set method called, false if not.
	 */
	public static boolean callSetMethod(Object bean, String name, String value)
	{
		Class beanCls = bean.getClass();
		Method methods[] = beanCls.getDeclaredMethods();
		
//System.out.println("bean's class is '" + beanCls.getName() + "'");
//System.out.println("bean has " + methods.length + " declared methods.");
//System.out.println("name='" + name + "' value='" + value + "'");

		// value is always string so class is "java.lang.String"

		for(Method m : methods)
		{
			String mname = m.getName();
			if (!Modifier.isPublic(m.getModifiers()))
			{
//System.out.println("Skipping method " + mname + " because not public");
				continue;
			}
			Class argtypes[] = m.getParameterTypes();
//System.out.println("Checking method '" + mname + "' with " + argtypes.length + " args.");
			if (argtypes.length != 1)
			{
//System.out.println("Skipping method with " + argtypes.length + " args.");
				continue;
			}
//System.out.println("methods arg is of type '" + argtypes[0].getName() + "'");
			
			if (mname.equalsIgnoreCase("set" + name))
			{
				Object arglist[] = new Object[1];
				String atype = argtypes[0].getName();
//System.out.println("Found matching method '" + mname + "'");
				if (atype.equals("java.lang.String"))
					arglist[0] = value;
				else if (atype.equals("int") || atype.equals("java.lang.Integer"))
				{
					try { arglist[0] = new Integer(Integer.parseInt(value)); }
					catch(NumberFormatException ex)
					{
//System.out.println("Method requires int but non-integer was passed.");
						return false;
					}
				}
				else if (atype.equals("boolean") || atype.equals("java.lang.Boolean"))
					arglist[0] = TextUtil.str2boolean(value);
				else if (atype.equals("java.util.Date"))
				{
					try { arglist[0] = sdf.parse(value); }
					catch(Exception ex)
					{
//System.out.println("Parse exception for date '" + value + "'");
						return false;
					}
				}	
				try
				{
					m.invoke(bean, arglist);
					return true;
				}
				catch (Exception ex)
				{
					System.err.println(ex.toString());
				}
			}
		}
		return false;
	}
	

	public static void main(String args[])
	{
		testbean b = new testbean();
		callSetMethod(b, "intval", "123456");
		callSetMethod(b, "BOOLVAL", "false");
		callSetMethod(b, "stringval", "this is a string");
		callSetMethod(b, "dateval", "2010/04/15-12:00:00");
	}
}
class testbean
{
	private void setIntval(float f) {}
	public void setIntVAL(int i1, int i2) { }
	public void setIntval(Integer i) { System.out.println("Setting intval to " + i); }
	public void setBoolVal(Boolean b) { System.out.println("Setting boolval to " + b); }
	public void setStringVal(String s) { System.out.println("Setting stringval to " + s); }
	public void setDateVal(Date d) { System.out.println("Setting dateval to " 
		+ BeanUtil.sdf.format(d)); }
}
