package decodes.tsdb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import decodes.cwms.CwmsTsId;

import decodes.tsdb.comprungui.TimeSeriesTableModel;
import decodes.tsdb.comprungui.TimeSeriesTableModel.ColumnInfo;
import decodes.tsdb.comprungui.TimeSeriesTablePanel;
import ilex.var.TimedVariable;
import lrgs.rtstat.hosts.LrgsConnectionPanel;


public final class LrgsConnectionPanelScaffold
{
	public static void main(String[] args) throws Exception
	{

		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JFrame window = new JFrame();
				Container content = window.getContentPane();
				content.setLayout(new BorderLayout());
				LrgsConnectionPanel lrgsPanel = new LrgsConnectionPanel();
				lrgsPanel.addComponentListener(
            new ComponentAdapter()
            {
                public void componentResized(ComponentEvent e)
                {
                    
                    System.out.println(lrgsPanel.getSize());
                    lrgsPanel.revalidate();
                    
                    System.out.println("cur: "+lrgsPanel.getSize());
                    System.out.println("min: "+lrgsPanel.getMinimumSize());
                    System.out.println("max: "+lrgsPanel.getMaximumSize());
                    System.out.println("pref:"+lrgsPanel.getPreferredSize());
                    System.out.println("panelLayout:"+lrgsPanel.getLayout());
					
                }
            });
				
		

				content.add(lrgsPanel, BorderLayout.NORTH);
				content.add(new LrgsConnectionPanel(), BorderLayout.CENTER);
				
				window.pack();
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setVisible(true);
			}
		});
	}

	public static CTimeSeries makeTs(String uniqueName, int intervalMinutes, int durationHours)
	{
		TimeSeriesIdentifier tsId = new CwmsTsId();
				try
				{

					tsId.setUniqueString(uniqueName);
					CTimeSeries cts = new CTimeSeries(tsId);
					cts.setUnitsAbbr("in");
					GregorianCalendar cal = new GregorianCalendar(2025, 6, 11, 0, 0, 0);
					Random r = new Random();
					for(int i = 0; i < (durationHours*60)/intervalMinutes; i++)
					{
						TimedVariable tv = new TimedVariable(cal.getTime(), r.nextDouble(), 0);
						cts.addSample(tv);
						cal.add(Calendar.MINUTE, intervalMinutes);
					}
					return cts;
				}
				catch (BadTimeSeriesException ex)
				{
					throw new RuntimeException(ex);
				}
	}
}
