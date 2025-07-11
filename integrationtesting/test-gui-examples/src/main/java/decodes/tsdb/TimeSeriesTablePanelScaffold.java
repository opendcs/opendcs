package decodes.tsdb;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import decodes.cwms.CwmsTsId;

import decodes.tsdb.comprungui.TimeSeriesTableModel;
import decodes.tsdb.comprungui.TimeSeriesTablePanel;
import ilex.var.TimedVariable;


public final class TimeSeriesTablePanelScaffold
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
				TimeSeriesTablePanel tsTable = new TimeSeriesTablePanel();
				final TimeSeriesTableModel model = tsTable.getTimeSeriesModel();
				
				content.add(tsTable, BorderLayout.CENTER);

				JButton addInputTs = new JButton("Add Input");
				addInputTs.addActionListener(e ->
				{
					model.addInput(makeTs("Alder Springs.Precip.Inst.15Minutes.0.raw", 15, 24));
				});

				JButton addOutputTs = new JButton("Add output");
				addOutputTs.addActionListener(e ->
				{
					model.addOutput(makeTs("Alder Springs.Precip.Inst.1Hour.0.raw", 60, 24));
				});

				JButton clearTimeSeries = new JButton("Clear Time Series");
				clearTimeSeries.addActionListener(e -> model.clearTimesSeries());

				JPanel controls = new JPanel();
				controls.add(addInputTs);
				controls.add(addOutputTs);
				controls.add(clearTimeSeries);

				content.add(controls, BorderLayout.NORTH);

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
