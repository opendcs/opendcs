/*
*	$Id$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:08  cvs
*	Added legacy code to repository
*	
*	Revision 1.3  2005/09/19 21:30:39  mmaloney
*	dev
*	
*	Revision 1.2  2005/01/11 15:13:45  mjmaloney
*	Added boolean option to ctor to get rid of snap-check.
*	
*	Revision 1.1  2004/11/16 21:01:59  mjmaloney
*	Created.
*	
*/
package ilex.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;
import java.awt.event.*;

/**
This class displays a scrolling list of events. It is usually placed
at the bottom of a GUI frame.
*/
public class EventsPanel extends JPanel
{
	private BorderLayout borderLayout1 = new BorderLayout();
	private JScrollPane eventsPane = new JScrollPane();
	EventsListModel listModel = new EventsListModel();
	JList eventsList = new JList(listModel);
	private JPanel jPanel1 = new JPanel();
	private FlowLayout flowLayout1 = new FlowLayout();
	JCheckBox snapCheck = new JCheckBox();

	private boolean includeSnapCheck = true;

	/** the state of the snap checkbox, if true, snap scroll pane to last evt */
	boolean snapToEnd;

	/**
	 * Constructor.
	 */
	public EventsPanel()
	{
		this(true);
	}

	/**
	 * Constructor to use if you don't want the snap checkbox at the bottom.
	 * @param includeSnapCheck set to false if you don't want the sna checkbox.
	 */
	public EventsPanel(boolean includeSnapCheck)
	{
		this.includeSnapCheck = includeSnapCheck;
		try 
		{
			jbInit();
			eventsList.setCellRenderer(new EventsCellRenderer());
			snapCheck.setSelected(true);
		}
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
	}

	/** Initialize GUI components */
	void jbInit() throws Exception 
	{
		this.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		snapCheck.setText("Snap to End");
		flowLayout1.setAlignment(FlowLayout.LEFT);
		flowLayout1.setVgap(5);
		eventsPane.getViewport().setBackground(Color.black);
    	eventsPane.setBorder(BorderFactory.createLoweredBevelBorder());
    	this.add(eventsPane, BorderLayout.CENTER);
		if (includeSnapCheck)
		{
			this.add(jPanel1,	BorderLayout.SOUTH);
			jPanel1.add(snapCheck, null);
		}
		eventsPane.getViewport().setView(eventsList);
	 }

	/**
	  Adds a line to the events panel, scrolling to end if snap is checked.
	  @param line the line to add
	*/
	public synchronized void addLine(String line)
	{
		final String toAdd = line;

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					// This changes the model, which fires the events that
					// enqueue messages to render the component.
					listModel.addLine(toAdd);

					// This enqueues another task to modify the scrolling.
					// Doing it this way ensures that the scrolling is done
					// after rendering.
					SwingUtilities.invokeLater(
						new Runnable()
						{
							public void run()
							{
								checkScroll();
							}
						});
				}
			});
	}

	/**
	 * If the snap checkbox is checked, this will snap the scroll pane
	 * so that the last event is visible.
	 */
	void checkScroll()
	{
		if (snapCheck.isSelected())
		{
			Dimension listdim = eventsList.getSize();
			JViewport vp = eventsPane.getViewport();
			Dimension vpdim = vp.getExtentSize();
			Point p = vp.getViewPosition();
			p.y = listdim.height - vpdim.height;
			if (p.y > 0)
			{
				vp.setViewPosition(p);
				//eventsList.scrollRectToVisible(
				//	new Rectangle(p.x, p.y, p.x + vpdim.width,
				//		p.y + vpdim.height));
			}
		}
	}
}

/**
Internal class that represents a vector of strings in a List Model.
*/
class EventsListModel extends AbstractListModel
{
	/** Max lines in the buffer */
	int maxlines = 1000;

	/** Retire chunks this size when the buffer is full. */
	int chunk = 100;

	/** Extension of Vector to make removeRange public */
	class myvec extends Vector
	{
		public void removeRange(int from, int to)
		{
			super.removeRange(from, to);
		}
	}

	/** The vector of strings to display */
	myvec lines;

	/** Constructor */
	EventsListModel()
	{
		lines = new myvec();
	}

	/** 
	 * Adds a line to the vector, retiring a chunk if the buffer is full. 
	 * @param line the string to add.
	 */
	void addLine(String line)
	{
		int from, to;
		if (lines.size() > maxlines)
		{
			lines.removeRange(0,chunk);
			from = 0;
		}
		else
			from = lines.size();
		to=lines.size();
		lines.add(line);
		fireIntervalAdded(this, from, to);
	}

	/**
	 * Get String element.
	 * @param idx the index to retrieve
	 * @return the String at the specified index.
	*/
	public Object getElementAt(int idx)
	{
		if (idx < 0 || idx >= lines.size())
			return "";
		else
			return lines.elementAt(idx);
	}

	/**
	 * @return number of lines in the buffer.
	 */
	public int getSize()
	{
		return lines.size();
	}
}

/**
Internal class for rendering events in different colors depending
on the priority.
*/
class EventsCellRenderer extends JLabel implements ListCellRenderer
{
	Color failureColor;
	Color warningColor;
	Color normalColor;

	EventsCellRenderer()
	{
		failureColor = new Color(255, 100, 100);
		warningColor = new Color(255, 255, 0);
		normalColor = new Color(200, 200, 200);

		setOpaque(true);
		setBackground(Color.black);
		setFont(new Font("Monospaced", Font.BOLD, 14));
		setBorder(BorderFactory.createLineBorder(Color.white,1));
	}

	public Component getListCellRendererComponent(
			JList list, Object value, int idx, boolean isSelected, boolean hasFocus)
	{
		if (idx == -1 || value == null)
			setText("rendered " + idx);
		else
		{
			String s = (String)value;
			setText(s);
			if (s.startsWith("FA")) // FAILURE or FATAL
				setForeground(failureColor);
			else if (s.startsWith("WA")) // WARNING
				setForeground(warningColor);
			else
				setForeground(normalColor);
		}
//System.out.println("Rendered '" + value + "'");

		return this;
	}
}
