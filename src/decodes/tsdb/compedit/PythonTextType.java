package decodes.tsdb.compedit;

import java.awt.Color;

import ilex.util.Logger;
import decodes.util.DecodesSettings;

/**
 * Types of text to display in different colors in script editor.
 */
public enum PythonTextType
{
	NormalText(DecodesSettings.instance().pyNormalColor, 0, "Normal Text"),
	Keywords(DecodesSettings.instance().pyKeywordColor, 0x0000FF, "Python Keyword"),
	BuiltIns(DecodesSettings.instance().pyBuiltinColor, 0xD2691E, "Python Built-in Function"),
	QuotedString(DecodesSettings.instance().pyQuotedColor, 0x00D000, "Quoted String"),
	TimeSeriesRole(DecodesSettings.instance().pyTsRoleColor, 0x8B0000, "CP Time Series Role"),
	Properties(DecodesSettings.instance().pyPropColor, 0x4B0082, "CP Property Name"),
	Comment(DecodesSettings.instance().pyCommentColor, 0x808000, "Comment Text"),
	CpFunction(DecodesSettings.instance().pyCpFuncColor, 0x8B4513, "CP Built-in Function");
	
	/** The color in which to display this type of text */
	private Color displayColor;
	private String desc;
	
	private PythonTextType(String hexColor, int defaultColor, String desc)
	{
		if (hexColor != null)
		{
			try { displayColor = new Color(Integer.parseInt(hexColor, 16)); }
			catch(Exception ex)
			{
				Logger.instance().warning("Illegal hexColor '" + hexColor
					+ "' in config file. Will use default.");
				displayColor = new Color(defaultColor);
			}
		}
		else
			displayColor = new Color(defaultColor);
		this.desc = desc;
	}

	public Color getDisplayColor()
	{
		return displayColor;
	}

	public String getDesc()
	{
		return desc;
	}
}
