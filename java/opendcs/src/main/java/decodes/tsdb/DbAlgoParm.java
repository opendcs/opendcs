/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb;

/**
This is a data-structure class holding meta-data for a single parameter
for an algorithm.
*/
public class DbAlgoParm
{
	/** The role name -- must be unique within an algorithm. */
	private String roleName;
	
	/** The parameter type -- one of the constant codes defined herein. */
	private String parmType;

	/**
	 * Constructor.
	 * @param roleName the role name, must be unique within the algorithm.
	 * @param parmType type code.
	*/
	public DbAlgoParm(String roleName, String parmType)
	{
		this.roleName = roleName;
		this.parmType = parmType;
	}

	/**
	 * Copy Constructor
	 */
	public DbAlgoParm(DbAlgoParm rhs)
	{
		this(rhs.roleName, rhs.parmType);
	}
	

	/**
	 * @return the role name.
	 */
	public String getRoleName() { return roleName; }

	/**
	 * Sets the role name.
	 * @param roleName the role name.
	 */
	public void setRoleName(String roleName)
	{
		this.roleName = roleName;
	}

	/**
	 * @return the parameter type.
	 */
	public String getParmType() { return parmType; }

	/**
	 * Sets the parameter type.
	 * @param parmType the parameter type.
	 */
	public void setParmType(String parmType)
	{
		this.parmType = parmType;
	}
}
