package org.opendcs.odcsapi.dao;

public class DbException extends Exception
{
	private static final long serialVersionUID = 4552935582551955795L;
	private String module = null;
	private Exception cause = null;
	
	public DbException(String msg)
	{
		super(msg);
	}
	
	public DbException(String module, Exception cause, String msg)
	{
		this(msg);
		this.module = module;
		this.cause = cause;
	}

	public String getModule()
	{
		return module;
	}

	public Exception getCause()
	{
		return cause;
	}
}
