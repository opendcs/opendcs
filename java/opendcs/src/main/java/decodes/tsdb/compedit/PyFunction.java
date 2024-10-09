package decodes.tsdb.compedit;

/**
 * Holds information about a python-callable function
 */
public class PyFunction
{
	private String name;
	private String signature;
	private String desc;
	
	public PyFunction(String name, String signature, String desc)
	{
		super();
		this.name = name;
		this.signature = signature;
		this.desc = desc;
	}

	public String getName()
	{
		return name;
	}

	public String getSignature()
	{
		return signature;
	}

	public String getDesc()
	{
		return desc;
	}
	
	
}
