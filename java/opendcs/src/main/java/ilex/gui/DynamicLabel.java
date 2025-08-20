/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:08  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/11/06 15:18:49  mmaloney
*  dev
*
*  Revision 1.3  2004/08/30 14:50:18  mjmaloney
*  Javadocs
*
*  Revision 1.2  2000/03/25 22:03:25  mike
*  dev
*
*  Revision 1.1  2000/03/24 02:15:26  mike
*  Created.
*
*/
package ilex.gui;

import java.awt.*;
import javax.swing.border.BevelBorder;
import javax.swing.*;

/**
* This class implements a field that can change dynamically as the
* program is run. It is displayed in Green on Black so it stands out
* from the background. The text is left-justified within the field width.
*/
public class DynamicLabel extends JPanel
{
	/** Width in characters */
	int charWidth;

	/** border shared by all instances */
	static BevelBorder bevel = null;
	/** the label */
	JLabel label;

	/** No-args constructor. */
	public DynamicLabel()
	{
		this("", 10);
	}

	/**
	* Constructor.
	* @param initialText initial text
	* @param charWidth character width
	*/
	public DynamicLabel( String initialText, int charWidth )
	{
		super();
		this.charWidth = charWidth;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		
		label = new JLabel(fillString(initialText, charWidth));
//		label.setHorizontalTextPosition(SwingConstants.LEFT);
		add(label);
		
		// Apply an inset bevel-border
		if (bevel == null)
			bevel = new BevelBorder(BevelBorder.LOWERED,
				Color.lightGray, Color.darkGray);
		setBorder(bevel);
		
		// Make font monospaced bold, but keep the same point size.
		Font oldfont = label.getFont();
		label.setFont(new Font("Monospaced", Font.BOLD, oldfont.getSize()));
		
		// Set panel color to green on black.
		label.setForeground(Color.green);
		setBackground(Color.black);
	}
	
	/**
	* Overrides setText in JLabel. The text is extended or truncated
	* to the proper length.
	* @param text the text
	*/
	public void setText( String text )
	{
		label.setText(fillString(text, charWidth));
	}
	
	/**
	* Fills a string
	* @param text the text
	* @param charWidth number of characters
	* @return String filled
	*/
	private static String fillString( String text, int charWidth )
	{
		StringBuffer sb = new StringBuffer(text);
		if (text.length() > charWidth)
			sb.setLength(charWidth);
		else
			for(int i=text.length(); i<charWidth; i++)
				sb.append(' ');
		return new String(sb);
	}
}
