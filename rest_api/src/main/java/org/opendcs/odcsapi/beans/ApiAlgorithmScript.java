package org.opendcs.odcsapi.beans;

public class ApiAlgorithmScript
{
	public static final char TYPE_INIT = 'I';
	public static final char TYPE_BEFORE = 'B';
	public static final char TYPE_TIMESLICE = 'T';
	public static final char TYPE_AFTER = 'A';
	public static final char TYPE_TOOLTIP = 'P';
	public static final char TYPE_UNDEFINED = 'U';
	
	private String text = "";
	private char scriptType = TYPE_UNDEFINED;
	
	
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
