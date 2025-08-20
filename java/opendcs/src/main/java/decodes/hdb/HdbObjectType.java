package decodes.hdb;

import decodes.sql.DbKey;

public class HdbObjectType
{
	private DbKey id = DbKey.NullKey;
	private String name = null;
	private String tag = null;
	private int parentOrder = 0;
	
	protected HdbObjectType(DbKey id, String name, String tag, int parentOrder)
	{
		super();
		this.id = id;
		this.name = name;
		this.tag = tag;
		this.parentOrder = parentOrder;
	}
	
	public DbKey getId()
	{
		return id;
	}
	public void setId(DbKey id)
	{
		this.id = id;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getTag()
	{
		return tag;
	}
	public void setTag(String tag)
	{
		this.tag = tag;
	}
	public int getParentOrder()
	{
		return parentOrder;
	}
	public void setParentOrder(int parentOrder)
	{
		this.parentOrder = parentOrder;
	}
}
