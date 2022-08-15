package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;

public class ApiConfigScript
{
	private String name = null;
	
	/** U=undefined, A=ascending, D=descending */
	private char dataOrder = 'U';
	
	private String headerType = null;
	
	private ArrayList<ApiConfigScriptSensor> scriptSensors = 
		new ArrayList<ApiConfigScriptSensor>();
	
	private ArrayList<ApiScriptFormatStatement> formatStatements =
		new ArrayList<ApiScriptFormatStatement>();

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public char getDataOrder()
	{
		return dataOrder;
	}

	public void setDataOrder(char dataOrder)
	{
		this.dataOrder = dataOrder;
	}

	public String getHeaderType()
	{
		return headerType;
	}

	public void setHeaderType(String headerType)
	{
		this.headerType = headerType;
	}

	public ArrayList<ApiConfigScriptSensor> getScriptSensors()
	{
		return scriptSensors;
	}

	public void setScriptSensors(ArrayList<ApiConfigScriptSensor> scriptSensors)
	{
		this.scriptSensors = scriptSensors;
	}

	public ArrayList<ApiScriptFormatStatement> getFormatStatements()
	{
		return formatStatements;
	}

	public void setFormatStatements(ArrayList<ApiScriptFormatStatement> formatStatements)
	{
		this.formatStatements = formatStatements;
	}
	
}
