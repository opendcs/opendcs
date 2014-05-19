/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.8  2013/03/25 17:50:54  mmaloney
 * dev
 *
 * Revision 1.7  2013/03/25 17:13:11  mmaloney
 * dev
 *
 * Revision 1.6  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.5  2013/03/25 15:02:20  mmaloney
 * dev
 *
 * Revision 1.4  2013/03/23 18:20:04  mmaloney
 * dev
 *
 * Revision 1.3  2013/03/23 18:01:03  mmaloney
 * dev
 *
 * Revision 1.2  2013/03/23 15:33:55  mmaloney
 * dev
 *
 * Revision 1.1  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb.procmonitor;

import ilex.gui.EventsPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import decodes.gui.SortingListTable;
import decodes.gui.TopFrame;

/**
 * Main frame for process status monitor GUI
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
@SuppressWarnings("serial")
public class ProcessMonitorFrame 
	extends TopFrame
{
	ProcStatTableModel model = null;
	EventsPanel eventsPanel = new EventsPanel();

	/**
	 * Constructor
	 */
	public ProcessMonitorFrame()
	{
		guiInit();
		pack();
		this.trackChanges("ProcessMonitorFrame");
	}
	
	private void guiInit()
	{
		this.setTitle("Process Monitor");
		model = new ProcStatTableModel(this);
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(new JLabel("Process Status Monitor"), BorderLayout.NORTH);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		SortingListTable processTable = new SortingListTable(model, model.widths);
		JScrollPane scrollPane = new JScrollPane(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(processTable);
		splitPane.setTopComponent(scrollPane);
		splitPane.setBottomComponent(eventsPanel);
		scrollPane.setPreferredSize(new Dimension(900, 300));
		eventsPanel.setPreferredSize(new Dimension(900, 300));
	}
	
	public void cleanupBeforeExit()
	{
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ProcessMonitorFrame f = new ProcessMonitorFrame();
//		f.centerOnScreen();
		Rectangle r = f.getBounds();
		f.setExitOnClose(true);
		f.launch(r.x, r.y, r.width, r.height);
	}
	
	public void launch( int x, int y, int w, int h )
	{
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final ProcessMonitorFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					myframe.cleanupBeforeExit();
					if (exitOnClose)
						System.exit(0);
				}
			});
	}

	public ProcStatTableModel getModel() { return model; }
	
	public synchronized void addEvent(String event)
	{
		eventsPanel.addLine(event);
	}

}

