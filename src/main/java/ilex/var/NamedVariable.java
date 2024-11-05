/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2006/06/28 11:53:48  mmaloney
*  dev
*
*  Revision 1.3  2004/08/31 16:35:48  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/08/30 14:50:35  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/11/22 15:14:17  mike
*  dev
*
*
*/
package ilex.var;

/**
* NamedVariable extends the functionality of ilex.var.Variable by adding
* a name.
*/
public class NamedVariable extends Variable
{
	private String name;

	/**
	* Constructs a default variable with no name.
	*/
	public NamedVariable( )
	{
		super();
		name = "";
	}

	/**
	* Copy constructor
	* @param nv the variable to copy
	* @throws NoConversionException
	*/
	public NamedVariable( NamedVariable nv ) throws NoConversionException
	{
		super(nv);
		name = nv.name;
	}

	/**
	* Constructs a copy of variable with a different name.
	* @param name the name
	* @param v the variable
	*/
	public NamedVariable( String name, Variable v )
	{
		super(v);
		this.name = name;
	}

	/**
	* Constructs a new byte variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, byte v )
	{
		super(v);
		this.name = name;
	}

	/** Constructs a new byte array variable */
/*
	public NamedVariable(String name, byte[] v)
	{
		super(v);
		this.name = name;
	}
*/

	/**
	* Constructs a new integer variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, int v )
	{
		super(v);
		this.name = name;
	}

	/** Constructs a new integer array variable */
/*
	public NamedVariable(String name, int v[])
	{
		super(v);
		this.name = name;
	}
*/

	/**
	* Constructs a new long integer variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, long v )
	{
		super(v);
		this.name = name;
	}

	/** Constructs a new long integer array variable */
/*
	public NamedVariable(String name, long[] v)
	{
		super(v);
		this.name = name;
	}
*/

	/**
	* Constructs a new floating point variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, float v )
	{
		super(v);
		this.name = name;
	}

	/** Constructs a new float array variable */
/*
	public NamedVariable(String name, float[] v)
	{
		super(v);
		this.name = name;
	}
*/

	/**
	* Constructs a new double variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, double v )
	{
		super(v);
		this.name = name;
	}

	/** Constructs a new double array variable */
/*
	public NamedVariable(String name, double[] v)
	{
		super(v);
		this.name = name;
	}
*/

	/**
	* Constructs a new string variable
	* @param name the name
	* @param v the value
	*/
	public NamedVariable( String name, String v )
	{
		super(v);
		this.name = name;
	}

	/**
	* Sets the name of this variable.
	* @param name the name
	*/
	public void setName( String name )
	{
		this.name = name;
	}

	/**
	* @return the name of this variable.
	*/
	public String getName( )
	{
		return name;
	}

	/**
	* @return a string of the form "name=value".
	*/
	public String toString( )
	{
		return name + "=" + getStringValue();
	}
}

