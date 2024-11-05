/*
*	$Id$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:16  cvs
*	Added legacy code to repository
*	
*	Revision 1.1  2004/04/22 20:04:47  mjmaloney
*	Added LQM & Events to the GUI.
*	
*/
package lritdcs;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;
import java.awt.event.*;

public class EventsPanel extends JPanel
{
	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane eventsPane = new JScrollPane();
	EventsListModel listModel = new EventsListModel();
	JList eventsList = new JList(listModel);
	JPanel jPanel1 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JCheckBox snapCheck = new JCheckBox();

	boolean snapToEnd;

	public EventsPanel()
	{
		try {
			jbInit();
			eventsList.setCellRenderer(new EventsCellRenderer());
			snapCheck.setSelected(true);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception {
		this.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		snapCheck.setText("Snap to End");
		flowLayout1.setAlignment(FlowLayout.LEFT);
    flowLayout1.setVgap(5);
		eventsPane.getViewport().setBackground(Color.black);
    	eventsPane.setBorder(BorderFactory.createLoweredBevelBorder());
    	this.add(eventsPane, BorderLayout.CENTER);
		this.add(jPanel1,	BorderLayout.SOUTH);
		jPanel1.add(snapCheck, null);
		eventsPane.getViewport().setView(eventsList);
	 }

	/**
	  Adds a line to the events panel, scrolling to end if snap is checked.
	*/
	public void addLine(String line)
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

	void checkScroll()
	{
//System.out.println("Check Scroll, num events = " + listModel.getSize());
		// if snap-checked, scroll so that new event is visible.
		if (snapCheck.isSelected())
		{
			Dimension listdim = eventsList.getSize();
			JViewport vp = eventsPane.getViewport();
			Dimension vpdim = vp.getExtentSize();
			Point p = vp.getViewPosition();
//System.out.println("Viewport position = ("+p.x+","+p.y+")");
//System.out.println("Viewport height="+vpdim.height+", list height=" + listdim.height);
			p.y = listdim.height - vpdim.height;
			if (p.y > 0)
			{
				vp.setViewPosition(p);
				//eventsList.scrollRectToVisible(
				//	new Rectangle(p.x, p.y, p.x + vpdim.width,
				//		p.y + vpdim.height));
//System.out.println("Set y to " + p.y);
			}
		}
	}
}

class EventsListModel extends AbstractListModel
{
	int maxlines = 1000;
	int chunk = 100;

	class myvec extends Vector
	{
		public void removeRange(int from, int to)
		{
			super.removeRange(from, to);
		}
	}
	myvec lines;

	EventsListModel()
	{
		lines = new myvec();
	}
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
	public Object getElementAt(int idx)
	{
		if (idx < 0 || idx >= lines.size())
			return "";
		else
			return lines.elementAt(idx);
	}
	public int getSize()
	{
		return lines.size();
	}
}

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
		setFont(new Font("Monospaced", Font.BOLD, 16));
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
