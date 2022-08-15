package opendcs.opentsdb.hydrojson.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TestBean
{
	private int value = 1;
	private String name = "two";
	
	public TestBean()
	{
		
	}
	
	public int getValue()
	{
		return value;
	}
	public void setValue(int value)
	{
		this.value = value;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	
	
}
