package opendcs.opentsdb.hydrojson.beans;

public class ApiScriptFormatStatement
{
	private int sequenceNum = 0;
	private String label = null;
	private String format = null;
	public int getSequenceNum()
	{
		return sequenceNum;
	}
	public void setSequenceNum(int sequenceNum)
	{
		this.sequenceNum = sequenceNum;
	}
	public String getLabel()
	{
		return label;
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public String getFormat()
	{
		return format;
	}
	public void setFormat(String format)
	{
		this.format = format;
	}
	
}
