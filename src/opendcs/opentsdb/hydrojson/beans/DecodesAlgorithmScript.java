package opendcs.opentsdb.hydrojson.beans;

import decodes.tsdb.ScriptType;

public class DecodesAlgorithmScript
{
	private String text = "";
	private char scriptType = ScriptType.Undefined.getDbChar();
	
	public String getText()
	{
		return text;
	}
	public void setText(String text)
	{
		this.text = text;
	}
	public char getScriptType()
	{
		return scriptType;
	}
	public void setScriptType(char scriptType)
	{
		this.scriptType = scriptType;
	}
}
