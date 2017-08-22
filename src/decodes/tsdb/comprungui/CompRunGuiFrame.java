/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 */
package decodes.tsdb.comprungui;

import ilex.gui.DateTimeCalendar;
import ilex.util.TeeLogger;
import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.TimeSeriesDAI;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.PeriodAxis;
import org.jfree.chart.axis.PeriodAxisLabelInfo;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;

import decodes.dbeditor.TraceDialog;
import decodes.dbeditor.TraceLogger;
import decodes.gui.TopFrame;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbCompResolver;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.DuplicateTimeSeriesException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.VarFlags;
import decodes.tsdb.compedit.ComputationsEditPanel;
import decodes.tsdb.compedit.ComputationsListPanel;
import decodes.tsdb.groupedit.TimeSeriesSelectDialog;
import decodes.util.DecodesSettings;

/**
 * The frame for running computations interactively.
 */
@SuppressWarnings("serial")
public class CompRunGuiFrame extends TopFrame
{
	static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	public static String description;
	public static String selectFromListLabel;
	private String removeFromListLabel;
	private String selectButtonLabel;
	private String selectTimeRun;
	private String fromLabel;
	private String toLabel;
	private String chartXLabel;
	private String runCompsButton;
	private String saveOutputButton;
	private String timeLabel;
	private String noCompSelectedErr;
	private String closeButtonLabel;// generic
	private String saveCompOutput;
	public static String okButtonLabel;// generic
	public static String cancelButtonLabel;// generic
	public static String dateTimeColumnLabel;
	public static String inputLabel;
	public static String outputLabel;

	private ComputationsTable mytable;
	private Vector<CTimeSeries> myoutputs = null;
	private TimeSeriesDb theDb = null;
	private DateTimeCalendar fromDTCal;
	private DateTimeCalendar toDTCal;
	private TimeSeriesCollection[] datasets;
	private JFreeChart mychart;
	private TimeSeriesTable timeSeriesTable;
	private String module = "RunComputationFrame";
	private ChartPanel chartPanel;

	private boolean standAloneMode = false;
	private boolean needToSave = false;
	private ComputationsEditPanel compEditParent;
	private String timeZoneStr;
	private RunComputationsFrameTester runCompFrametester;
	private ComputationsListDialog computationsListDialog = null;
	private JButton traceButton = new JButton("Trace Execution");

	private TraceDialog traceDialog = null;

	/**
	 * Constructor
	 * 
	 * @param standAloneMode
	 *            True if running from launcher or tester. False if running
	 *            inside compedit.
	 */
	public CompRunGuiFrame(boolean standAloneMode)
	{
		super();

		this.standAloneMode = standAloneMode;

		labels = RunComputationsFrameTester.getLabels();
		genericLabels = RunComputationsFrameTester.getGenericLabels();
		timeZoneStr = DecodesSettings.instance().sqlTimeZone;
		timeZoneStr = timeZoneStr == null ? "UTC" : timeZoneStr;
		setAllLabels();
		chartXLabel = "Time";

		Date tempDate = new Date();
		fromDTCal = new DateTimeCalendar(fromLabel, null, "dd MMM yyyy", timeZoneStr);
		toDTCal = new DateTimeCalendar(toLabel, tempDate, "dd MMM yyyy", timeZoneStr);

		JPanel mycontent = new JPanel();
		mycontent.setLayout(new BoxLayout(mycontent, BoxLayout.Y_AXIS));
		this.setContentPane(mycontent);
		this.setTitle(labels.getString("RunComputationsFrame.frameTitle"));
		this.trackChanges("runcomps");
		// this.setSize(750,825);//800
		mycontent.add(listPanel());
		mycontent.add(timePanel());
		mycontent.add(getChart());
		mycontent.add(getTable());
		mycontent.add(closePanel());
		pack();

		// Default operation is to do nothing when user hits 'X' in
		// upper right to close the window. We will catch the closing
		// event and do the same thing as if user had hit close.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				doClose();
			}
		});
		exitOnClose = true;

	}

	private void setAllLabels()
	{
		description = labels.getString("RunComputationsFrame.description");
		selectFromListLabel = labels.getString("RunComputationsFrame.selectFromListLabel");
		removeFromListLabel = labels.getString("RunComputationsFrame.removeFromListLabel");
		selectButtonLabel = labels.getString("RunComputationsFrame.selectButtonLabel");
		selectTimeRun = labels.getString("RunComputationsFrame.selectTimeRun");
		fromLabel = labels.getString("RunComputationsFrame.fromLabel");
		toLabel = labels.getString("RunComputationsFrame.toLabel");
		chartXLabel = labels.getString("RunComputationsFrame.chartXLabel");
		runCompsButton = labels.getString("RunComputationsFrame.runCompsButton");
		saveOutputButton = labels.getString("RunComputationsFrame.saveOutputButton");
		timeLabel = labels.getString("RunComputationsFrame.timeLabel");
		noCompSelectedErr = labels.getString("RunComputationsFrame.noCompSelectedErr");
		closeButtonLabel = genericLabels.getString("close");
		saveCompOutput = labels.getString("RunComputationsFrame.saveCompOutput");
		okButtonLabel = genericLabels.getString("OK");
		cancelButtonLabel = genericLabels.getString("cancel");
		dateTimeColumnLabel = labels.getString("TimeSeriesTable.dateTimeColumnLabel") + " (" + timeZoneStr
			+ ")";
		inputLabel = labels.getString("RunComputationsFrame.inputLabel");
		outputLabel = labels.getString("RunComputationsFrame.outputLabel");
	}

	/**
	 * When lauch from Comp Edit GUI need to set the parent so that we can get
	 * the DbComputation obj when the user presses Run Computation button.
	 * 
	 * @param compEdit
	 *            the parent of this frame
	 */
	public void setParent(ComputationsEditPanel compEdit)
	{
		compEditParent = compEdit;
	}

	private JPanel listPanel()
	{
		JPanel list = new JPanel();
		if (!standAloneMode)
			return list;

		list.setPreferredSize(new Dimension(600, 200));
		list.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), labels
			.getString("RunComputationsFrame.selectCompRunTitle")));
		list.setLayout(new BorderLayout());
		JScrollPane myscroll = new JScrollPane();
		list.add(myscroll, BorderLayout.CENTER);
		JPanel listButtonPanel = new JPanel();
		list.add(listButtonPanel, BorderLayout.EAST);
		listButtonPanel.setLayout(new GridBagLayout());
		JButton selectButton = new JButton(selectButtonLabel);
		JButton removeButton = new JButton(removeFromListLabel);
		selectButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectButtonPressed();

			}
		});
		removeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				removeButtonPressed();

			}
		});
		listButtonPanel.add(selectButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTH,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		listButtonPanel.add(removeButton, new GridBagConstraints(0, 1, 1, 1, 0, 1, GridBagConstraints.NORTH,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		mytable = new ComputationsTable();

		myscroll.add(mytable);
		myscroll.setViewportView(mytable);

		if (!standAloneMode)
		{
			selectButton.setEnabled(false);
			removeButton.setEnabled(false);
		}
		return list;
	}

	private JPanel timePanel()
	{
		JPanel time = new JPanel();
		time.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), selectTimeRun));
		time.setLayout(new BorderLayout());

		JPanel timehalf = new JPanel();
		timehalf.setLayout(new GridBagLayout());
		timehalf.add(fromDTCal, new GridBagConstraints(0, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(new JLabel(" (" + timeZoneStr + ")"), new GridBagConstraints(1, 0, 1, 1, 0, 0,
			GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(toDTCal, new GridBagConstraints(0, 1, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		time.add(timehalf, BorderLayout.WEST);

		JPanel runhalf = new JPanel();
		runhalf.setLayout(new GridBagLayout());
		JButton runButton = new JButton(runCompsButton);
		runButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				runButtonPressed();
			}
		});
		JButton saveButton = new JButton(saveOutputButton);
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveButtonPressed();
			}
		});
		traceButton.setEnabled(false);
		traceButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				traceButtonPressed();
			}
		});

		runhalf.add(runButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		runhalf.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		runhalf.add(traceButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		time.add(runhalf, BorderLayout.EAST);
		return time;
	}

	protected void traceButtonPressed()
	{
		traceDialog.setVisible(true);
	}

	/** Returns the chart panel */
	public ChartPanel getChart()
	{
		mychart = ChartFactory.createTimeSeriesChart("", timeLabel, " ",// Name
																		// of
																		// the
																		// site
																		// plus
																		// timeseries
																		// +
																		// units
			null, // dateset
			true, // legend
			true, // tooltip
			false // url
			);
		this.datasets = new TimeSeriesCollection[2];

		ChartPanel mypanel = new ChartPanel(mychart);
		chartPanel = mypanel;
		mypanel.setPreferredSize(new java.awt.Dimension(600, 500));

		mypanel.setDomainZoomable(true);
		mypanel.setRangeZoomable(true);

		Border border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4),
			BorderFactory.createEtchedBorder());
		mypanel.setBorder(border);
		return mypanel;
	}

	/**
	 * This methods plots all data on chart.
	 * 
	 * 
	 */
	private void plotDataOnChart(Vector<CTimeSeries> ctsList, int inputs)
	{
		if (ctsList == null || ctsList.isEmpty())
			return;
		CTimeSeries correctedTs = ctsList.get(0);
		// Here we get the data to plot from the
		// TimeSeries Info read from database
		// to set the Y axis (left and right)
		// we will plot the main time series plus reference points
		// reference point will go on right if units are diferent
		mychart = ChartFactory.createTimeSeriesChart("", chartXLabel, correctedTs.getUnitsAbbr(),// Main
																									// timeseries
																									// units
			null, // dataset
			true, // legend
			true, // tooltip
			false // url
			);
		this.chartPanel.setChart(mychart);
		// Check if we have data on the main correctedTs. If no data
		// do not plot anything.
		if (correctedTs.size() == 0)
			return;
		XYDataset[] xyDatasets = new XYDataset[ctsList.size()];
		HashMap<String, Integer> dataHashMap = new HashMap<String, Integer>();
		this.datasets = new TimeSeriesCollection[ctsList.size()];
		DecimalFormat numFormat = new DecimalFormat("###0.00");
		// We want to paint the outputs on top of the inputs, we also want the
		// inputs to start showing on the right, so we need to
		// reset the order of rendering the lines, by default is Reverse, which
		// means: the plot renders the primary dataset last (so that the
		// primary dataset overlays the secondary datasets).
		// To reverse this set it to forward. NOW if we have many inputs and
		// outputs we still have some overlay problems.
		mychart.getXYPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		mychart.getXYPlot().setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
		for (int i = 0; i < ctsList.size(); i++)
		{
			CTimeSeries tempTs = ctsList.get(i);
			if (tempTs.size() == 0)
				continue; // Continue if the TS has no values to plot
			TimeSeriesIdentifier tsid = tempTs.getTimeSeriesIdentifier();
			if (tsid == null)
				continue;

			String timeSeriesName;
			if (i < inputs)
				timeSeriesName = inputLabel + tsid.getUniqueString();
			else
				timeSeriesName = outputLabel + tsid.getUniqueString();

			if (correctedTs.getUnitsAbbr().equals(tempTs.getUnitsAbbr()) && i == 0)
			{
				// When eng units are the same as the
				// main eng units plot it on the left y axis, else
				// plot it on the right y axis
				xyDatasets[i] = createDataset(i, timeSeriesName + " (" + tempTs.getUnitsAbbr() + ")", tempTs,
					false);
				mychart.getXYPlot().setDataset(xyDatasets[i]); // Y left axis

				XYPlot plot = mychart.getXYPlot();
				plot.getRangeAxis().setLabel(" ( " + (tempTs.getUnitsAbbr()).toUpperCase() + " )");
				XYLineAndShapeRenderer rr = null;
				XYItemRenderer renderer = plot.getRenderer();
				SimpleDateFormat df1 = new SimpleDateFormat("ddMMMyyyy HH:mm");
				df1.setTimeZone(TimeZone.getTimeZone(timeZoneStr));
				renderer
					.setToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", df1, numFormat));
				renderer.setSeriesPaint(i, Color.black);// first one black
				if (renderer instanceof XYLineAndShapeRenderer)
				{
					rr = (XYLineAndShapeRenderer) renderer;
					// rr.setBaseShapesVisible(true);
					rr.setBaseShapesVisible(false);
					// rr.setShapesFilled(true);
					rr.setShapesFilled(false);
				}
				plot.setRenderer(i, rr);
			}
			else if (correctedTs.getUnitsAbbr().equals(tempTs.getUnitsAbbr()) && i > 0)
			{
				xyDatasets[0] = createDataset(0, timeSeriesName + " (" + tempTs.getUnitsAbbr() + ")", tempTs,
					true);
				mychart.getXYPlot().setDataset(0, xyDatasets[0]);
			}
			else
			{ // Y right axis, here we can add more Y right axis
				// Units are diferent that the correctedTs
				// Find out if we have this eng units in the dataHashMap
				Integer indexOfEngUnits = (Integer) dataHashMap.get(tempTs.getUnitsAbbr());
				if (indexOfEngUnits == null)
				{ // new eng units
					dataHashMap.put(tempTs.getUnitsAbbr(), i);
					xyDatasets[i] = createDataset(i, timeSeriesName + " (" + tempTs.getUnitsAbbr() + ")",
						tempTs, false);
					XYPlot plot = mychart.getXYPlot();
					NumberAxis axis2 = new NumberAxis(" ( " + (tempTs.getUnitsAbbr()).toUpperCase() + " )");
					axis2.setAutoRangeIncludesZero(false);
					plot.setDataset(i, xyDatasets[i]);
					plot.setRangeAxis(i, axis2);
					plot.mapDatasetToRangeAxis(i, i);
					XYLineAndShapeRenderer rr = null;
					XYItemRenderer renderer = new XYLineAndShapeRenderer();
					SimpleDateFormat df1 = new SimpleDateFormat("ddMMMyyyy HH:mm");
					df1.setTimeZone(TimeZone.getTimeZone(timeZoneStr));
					renderer.setToolTipGenerator(new StandardXYToolTipGenerator("{0}: ({1}, {2})", df1,
						numFormat));
					if (renderer instanceof XYLineAndShapeRenderer)
					{
						rr = (XYLineAndShapeRenderer) renderer;
						// rr.setBaseShapesVisible(true);
						rr.setBaseShapesVisible(false);
						// rr.setShapesFilled(true);
						rr.setShapesFilled(false);
					}
					plot.setRenderer(i, rr);
				}
				else
				{
					int indexForEngUnits = indexOfEngUnits.intValue();
					xyDatasets[indexForEngUnits] = createDataset(indexForEngUnits, timeSeriesName + " ("
						+ tempTs.getUnitsAbbr() + ")", tempTs, true);
					mychart.getXYPlot().setDataset(indexForEngUnits, xyDatasets[indexForEngUnits]);
				}
			}
		}
		XYPlot plot = mychart.getXYPlot();
		plot.clearRangeMarkers(); // clear all previous markers
		plot = mychart.getXYPlot();
		NumberAxis axisLeftY = (NumberAxis) plot.getRangeAxis();// gets the Y
		// axis on the
		// (left) side
		// TickUnitSource units = NumberAxis.createIntegerTickUnits();
		// axisLeftY.setStandardTickUnits(units);
		// Include the number zero on the Main Y axis (left)
		axisLeftY.setAutoRangeIncludesZero(false);
	}

	/**
	 * Create the Time Series Data set with all data read from the DB.
	 * 
	 * @param index
	 * @param timeSeriesName
	 * @param tsIn
	 * @param sameEngUnits
	 * @return
	 */
	private XYDataset createDataset(int index, String timeSeriesName, CTimeSeries tsIn, boolean sameEngUnits)
	{
		String namePlusEuAbbr = timeSeriesName;
		TimeSeries series = new TimeSeries(namePlusEuAbbr, Minute.class);
		TimeZone timezone = TimeZone.getTimeZone(timeZoneStr);
		series.clear();
		for (int i = 0; i < tsIn.size(); i++)
		{
			TimedVariable tv = tsIn.sampleAt(i);
			try
			{ // get flag and verify if value rejected, if rejected
				// do not plot it

				double value = tv.getDoubleValue();
				Date timestamp = tv.getTime();
				series.addOrUpdate(new Minute(timestamp, timezone), value);

			}
			catch (NoConversionException e)
			{
				Logger.instance().log(Logger.E_WARNING, e.toString());
			}
		}
		if (sameEngUnits)
		{ // add to a dataset already created
			this.datasets[index].addSeries(series);
			return this.datasets[index];
		}
		else
		{
			this.datasets[index] = new TimeSeriesCollection();
			this.datasets[index].addSeries(series);
		}
		return this.datasets[index];
	}

	/**
	 * Create the X axis "Domain Axis" according to the first time and last time
	 * of the CTimeSeries.
	 * 
	 * @param plot
	 * @param ts
	 */
	public void setDomainXAxisLabels(XYPlot plot, CTimeSeries ts)
	{
		if (plot == null || ts == null)
			return;
		long firstTsTime = 0;
		long lastTsTime = 0;
		long timeRange = 0;
		TimedVariable tvFrom = ts.sampleAt(0);
		if (tvFrom != null)
		{
			firstTsTime = tvFrom.getTime().getTime();
		}
		TimedVariable tvTo = ts.sampleAt(ts.size() - 1);
		if (tvTo != null)
		{
			lastTsTime = tvTo.getTime().getTime();
		}
		timeRange = lastTsTime - firstTsTime;
		// Need to verify the first time and end time of the correctedTs
		// Depending on the time range will create the Domain Axis (x)
		// Hour or Hours range
		// Day or days range
		// week range
		// month range
		// year range
		long hourRange = 3600000L;// =milliseconds //3600=minutes;
		long dayRange = 86400000L; // 3600*24=minutes; //1000 * 60 * 60 * 24
		long weekRange = 86400000L * 7;
		long monthRange = 86400000L * 31; // 3600*24*31=minutes
		long yearRange = 86400000L * 31 * 12; // 3600*24*365 = minutes;
		// 31536000000
		PeriodAxis domainPeriodAxis = new PeriodAxis("");
		// domainPeriodAxis.setAutoRangeTimePeriodClass(Hour.class);
		// domainPeriodAxis.setAutoRangeTimePeriodClass(Minute.class);
		domainPeriodAxis.setTimeZone(TimeZone.getTimeZone(timeZoneStr));

		// To create a subtitle under the x axis label
		PeriodAxisLabelInfo[] info = new PeriodAxisLabelInfo[2];
		if (timeRange <= hourRange)
		{
			info = new PeriodAxisLabelInfo[1];
			info[0] = new PeriodAxisLabelInfo(Hour.class, new SimpleDateFormat("HH:mm"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		else if (timeRange <= dayRange)
		{
			info[0] = new PeriodAxisLabelInfo(Hour.class, new SimpleDateFormat("HH"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
			info[1] = new PeriodAxisLabelInfo(Day.class, new SimpleDateFormat("ddMMMyyyy"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, true, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		else if (timeRange <= weekRange)
		{
			info[0] = new PeriodAxisLabelInfo(Hour.class, new SimpleDateFormat("HH"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
			info[1] = new PeriodAxisLabelInfo(Day.class, new SimpleDateFormat("ddMMMyyyy"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, true, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		else if (timeRange <= monthRange)
		{
			info[0] = new PeriodAxisLabelInfo(Day.class, new SimpleDateFormat("d"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
			info[1] = new PeriodAxisLabelInfo(Month.class, new SimpleDateFormat("MMMyyyy"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, true, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		else if (timeRange <= yearRange)
		{
			info[0] = new PeriodAxisLabelInfo(Week.class, new SimpleDateFormat("W"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
			info[1] = new PeriodAxisLabelInfo(Month.class, new SimpleDateFormat("MMMyyyy"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, true, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		else
		{
			info[0] = new PeriodAxisLabelInfo(Month.class, new SimpleDateFormat("MMM"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, false, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
			info[1] = new PeriodAxisLabelInfo(Year.class, new SimpleDateFormat("yyyy"),
				PeriodAxisLabelInfo.DEFAULT_INSETS, PeriodAxisLabelInfo.DEFAULT_FONT,
				PeriodAxisLabelInfo.DEFAULT_LABEL_PAINT, true, PeriodAxisLabelInfo.DEFAULT_DIVIDER_STROKE,
				PeriodAxisLabelInfo.DEFAULT_DIVIDER_PAINT);
		}
		domainPeriodAxis.setLabelInfo(info);
		plot.setDomainAxis(domainPeriodAxis);
	}

	/** Sets the Database used here */
	public void setDb(TimeSeriesDb mydb)
	{
		this.theDb = mydb;
		timeSeriesTable.setTsdb(mydb);
	}

	private void runButtonPressed()
	{

		if (fromDTCal.getDate() == null || toDTCal.getDate() == null)
			return;
		Vector<CTimeSeries> inputs = new Vector<CTimeSeries>();
		Vector<CTimeSeries> outputs = new Vector<CTimeSeries>();
		Vector<DbComputation> compVector = new Vector<DbComputation>();

		if (!standAloneMode)
		{ // Get computation Obj from ComputationsEditPanel
			DbComputation dbComp = compEditParent.getEditedDbComputation();
			if (dbComp != null)
				compVector.add(dbComp);
		}
		else
		{
			compVector.addAll(mytable.mymodel.myvector);
		}
		if (compVector.size() == 0)
		{
			showError(noCompSelectedErr);
			return;
		}

		String groupCompName = null;
		TsGroup compGroup = null;
		for (DbComputation comp : compVector)
		{
			if (comp.hasGroupInput())
			{
				Logger.instance().debug3("comp " + comp.getName() + " has group input. ");
				if (compGroup == null)
				{
					compGroup = comp.getGroup();
					groupCompName = comp.getName();
					Logger.instance().debug3(
						"Will execute with group " + groupCompName + ", id=" + comp.getGroupId());
				}
				else if (compGroup.getGroupId() != comp.getGroupId())
				{
					showError("Multiple computations that are triggered from "
						+ "different time-series groups cannot be run together "
						+ "in the GUI: computation '" + groupCompName + "' uses group '"
						+ compGroup.getGroupName() + "', computation '" + comp.getName() + "' uses group '"
						+ comp.getGroupName()
						+ "'. -- Please de-select one of these computations to proceed.");
					return;
				}
				else
					Logger.instance().debug3(
						"comp " + comp.getName() + " also uses group id=" + comp.getGroupId());
			}
		}
		if (groupCompName == null)
			Logger.instance().debug3("None of the selected computations uses a group.");
		ArrayList<TimeSeriesIdentifier> groupTsIds = new ArrayList<TimeSeriesIdentifier>();
		if (compGroup != null)
		{
			try
			{
				DbComputation comp = compVector.get(0);
				DbCompParm firstInput = null;
				for (Iterator<DbCompParm> pit = comp.getParms(); pit.hasNext();)
				{
					DbCompParm parm = pit.next();
					if (parm.isInput())
					{
						firstInput = parm;
						break;
					}
				}
				if (firstInput == null)
				{
					showError("The computation has no inputs!");
					return;
				}

				// Read a fresh copy of the group from the DB
				compGroup = theDb.getTsGroupById(compGroup.getGroupId());
				// Expand the group and then filter it by the 1st input parm.
				ArrayList<TimeSeriesIdentifier> tsids = theDb.expandTsGroup(compGroup);

				// Collect the transformed tsids in a hash set to remove any
				// duplicates
				// that may result by transformation.
				TreeSet<TimeSeriesIdentifier> transformedTsids = new TreeSet<TimeSeriesIdentifier>();
				for (Iterator<TimeSeriesIdentifier> tsidit = tsids.iterator(); tsidit.hasNext();)
				{
					TimeSeriesIdentifier tsid = tsidit.next();

					// Transform the tsid by the parm. If the result is the same
					// as the original, that means that the parm 'matches'.
					TimeSeriesIdentifier transformed;
					try
					{
						transformed = theDb.transformTsidByCompParm(tsid, firstInput, false, false, "");
					}
					catch (NoSuchObjectException e)
					{
						tsidit.remove();
						continue;
					}
					catch (BadTimeSeriesException e)
					{
						// Can't happen because create flag is false.
						continue;
					}
					if (transformed == null)
					{
						tsidit.remove();
						continue;
					}
					Logger.instance().debug3("   original='" + tsid.getUniqueString() + "'");
					Logger.instance().debug3("transformed='" + transformed.getUniqueString() + "'");

					transformedTsids.add(transformed);
					Logger.instance().debug3(
						"transformedTsids now has " + transformedTsids.size() + " members.");
				}

				TimeSeriesSelectDialog dlg = new TimeSeriesSelectDialog(theDb, false, this);
				dlg.setMultipleSelection(true);
				Logger.instance().debug3(
					"Setting selection list with " + transformedTsids.size() + " entries.");
				dlg.setTimeSeriesList(transformedTsids);
				dlg.setTitle("Select time series for Execution.");
				launchDialog(dlg);
				TimeSeriesIdentifier[] tsidarray = dlg.getSelectedDataDescriptors();
				for (TimeSeriesIdentifier tsid : tsidarray)
					groupTsIds.add(tsid);

				Logger.instance().debug3("Will execute with the following time-series: ");
				for (TimeSeriesIdentifier tsid : groupTsIds)
					Logger.instance().debug3("   " + tsid.getUniqueString());

				ArrayList<DbComputation> concreteGroupComps = new ArrayList<DbComputation>();
				StringBuilder errorMsgs = new StringBuilder();
				for (Iterator<DbComputation> compit = compVector.iterator(); compit.hasNext();)
				{
					comp = compit.next();
					if (!comp.hasGroupInput())
						continue;

					// This is a group computation. Use resolver to make
					// a concrete copy, and then add it to concreteGroupComps.
					// First call compit.remote() to remove the abstract copy.
					compit.remove();

					for (TimeSeriesIdentifier tsid : groupTsIds)
					{
						try
						{
							DbComputation concrete = DbCompResolver.makeConcrete(theDb, tsid, comp, true);
							concreteGroupComps.add(concrete);
						}
						catch (NoSuchObjectException ex)
						{
							errorMsgs.append("Cannot execute '" + comp.getName() + "' for time series '"
								+ tsid.getUniqueString() + "': " + ex.getMessage() + "\r\n");
						}
					}
				}
				compVector.addAll(concreteGroupComps);
			}
			catch (DbIoException ex)
			{
				showError("Cannot expand group '" + compGroup.getGroupName() + "': " + ex);
				return;
			}
		}

		// Create a trace logger and put in the pipeline with tee logger.
		Logger origLogger = Logger.instance();
		TraceLogger traceLogger = new TraceLogger(origLogger.getProcName());
		TeeLogger teeLogger = new TeeLogger(origLogger.getProcName(), origLogger, traceLogger);
		traceLogger.setMinLogPriority(Logger.E_DEBUG3);
		Logger.setLogger(teeLogger);

		// Create the one-time trace dialog if not already done.
		if (traceDialog == null)
		{
			traceDialog = new TraceDialog(this, true);
			traceDialog.setTraceType("Computation Run");
		}
		traceDialog.clear();
		traceLogger.setDialog(traceDialog);

		// Flush the text area inside trace dialog
		// Create the trace logger here and put in pipe with tee logger.
		// Put trace dialog reference in trace logger.

		needToSave = true;
		for (DbComputation comp : compVector)
		{
			DataCollection runme = new DataCollection();
			// ArrayList<Integer> outputIDs = new ArrayList<Integer>();
			ArrayList<DbCompParm> outputParms = new ArrayList<DbCompParm>();

			boolean lowerBoundClosed = true;
			boolean upperBoundClosed = false;
			lowerBoundClosed = TextUtil.str2boolean(comp.getProperty("aggLowerBoundClosed"));
			upperBoundClosed = TextUtil.str2boolean(comp.getProperty("aggUpperBoundClosed"));

			// Get all inputs and expand them
			for (Iterator<DbCompParm> parmIt = comp.getParms(); parmIt.hasNext();)
			{
				DbCompParm parm = parmIt.next();

				// check for null on parm.getAlgoParmType
				if (parm.isInput() && parm.getSiteDataTypeId() != null && !parm.getSiteDataTypeId().isNull())
				{

					CTimeSeries ts = new CTimeSeries(parm);
					ts.setModelRunId(comp.getModelRunId());
					TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
					try
					{
						timeSeriesDAO.fillTimeSeries(ts, fromDTCal.getDate(), toDTCal.getDate(),
							lowerBoundClosed, upperBoundClosed, false);
					}
					catch (Exception ex)
					{
						String msg = module + " Exception filling input timeseries in "
							+ "runButtonPressed() " + ex;
						showError(msg);
						System.err.println(msg);
						ex.printStackTrace(System.err);
						continue;
					}
					finally
					{
						timeSeriesDAO.close();
					}
					for (int pos = 0; pos < ts.size(); pos++)
						VarFlags.setWasAdded(ts.sampleAt(pos));

					try
					{
						runme.addTimeSeries(ts);
						inputs.add(ts);
					}
					catch (DuplicateTimeSeriesException e)
					{
						// MJM some comps, like monthly delta may
						// Have the same TS as two separate inputs.
					}
				}
				else if (parm.isOutput())
				{
					// Record all the outputs to be sorted later
					outputParms.add(parm);
				}
			}
			try
			{
				Logger.instance().info(
					"Running computation " + comp.getName() + " modelRunId=" + comp.getModelRunId()
						+ ", with parms: ");
				for (Iterator<DbCompParm> dcpit = comp.getParms(); dcpit.hasNext();)
				{
					DbCompParm dcp = dcpit.next();
					CTimeSeries cts = runme.getTimeSeries(dcp.getSiteDataTypeId(), dcp.getInterval(),
						dcp.getTableSelector(), comp.getModelRunId());
					TimeSeriesIdentifier tsid = cts != null ? cts.getTimeSeriesIdentifier() : null;

					Logger.instance().info(
						"   "
							+ dcp.getRoleName()
							+ ":sdi="
							+ dcp.getSiteDataTypeId()
							+ ",intv="
							+ dcp.getInterval()
							+ ",tsel="
							+ dcp.getTableSelector()
							+ ",modId="
							+ dcp.getModelId()
							+ (cts == null ? " no existing TimeSeries" : " Existing TS with " + cts.size()
								+ " values in it ")
							+ (tsid == null ? "(no tsid)" : "and ts_id key=" + tsid.getKey() + " "
								+ tsid.getUniqueString()));
				}
				// run inputs through computation
				comp.prepareForExec(theDb);
				comp.apply(runme, theDb);
			}
			catch (DbCompException e)
			{
				// e.printStackTrace();
				showError(module + " DbCompException in " + "runButtonPressed() " + e.getMessage());
				continue;
			}
			catch (DbIoException e)
			{
				showError(module + " DbIOException in " + "runButtonPressed() " + e.getMessage());
				continue;
			}
			catch (NoSuchObjectException e)
			{
				// e.printStackTrace();
				showError(module + " NoSuchObjectException in " + "runButtonPressed() " + e.getMessage());
				continue;
			}

			// Get all outputs & add outputs to total lists;
			for (DbCompParm parm : outputParms)
			{
				boolean found = false;
				for (CTimeSeries cts : runme.getAllTimeSeries())
					if (cts.getSDI() == parm.getSiteDataTypeId()
						&& TextUtil.strEqual(cts.getInterval(), parm.getInterval())
						&& TextUtil.strEqual(cts.getTableSelector(), parm.getTableSelector()))
					{
						outputs.add(cts);
						Logger.instance().info(
							"After running, Found output sdi=" + cts.getSDI() + ", size=" + cts.size()
								+ ", units=" + cts.getUnitsAbbr());
						found = true;
						break;
					}
				if (!found)
					Logger.instance().info("No time series found for output role " + parm.getRoleName());
			}
		}

		Vector<CTimeSeries> both = new Vector<CTimeSeries>();
		for (CTimeSeries cts : inputs)
		{
			if (!both.contains(cts))
			{
				both.add(cts);
			}
		}

		for (CTimeSeries cts : outputs)
		{
			if (both.contains(cts))
				continue;

			TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			try
			{
				// We need to get meta data to display the axes.
				// But if units are already defined for an output, don't change
				// them.
				String oldUnits = cts.getUnitsAbbr();
				timeSeriesDAO.fillTimeSeriesMetadata(cts);
				if (oldUnits != null && !oldUnits.equalsIgnoreCase("unknown"))
					cts.setUnitsAbbr(oldUnits);
				Logger.instance().info(
					"After fill - Output TS: " + cts.getDisplayName() + ", nsamps=" + cts.size() + ", units="
						+ cts.getUnitsAbbr());
			}
			catch (DbIoException e)
			{
				Logger.instance().warning(
					module + " DbIoException in " + "runButtonPressed() filling outputs " + e.getMessage());
				continue;
			}
			catch (BadTimeSeriesException e)
			{
				Logger.instance().warning(
					module + " BadTimeSeriesException in " + "runButtonPressed() filling outputs "
						+ e.getMessage());
				continue;
			}
			finally
			{
				timeSeriesDAO.close();
			}

			both.add(cts);
		}

		// Stop trace logger and remove frome pipeline
		traceLogger.setDialog(null);
		Logger.setLogger(origLogger);
		traceLogger = null;
		teeLogger = null;
		traceButton.setEnabled(true);

		myoutputs = outputs;
		plotDataOnChart(both, inputs.size());
		timeSeriesTable.setInOut(inputs, outputs);
	}

	private JScrollPane getTable()
	{
		JScrollPane myscrollpane = new JScrollPane();
		myscrollpane.setPreferredSize(new Dimension(600, 400));
		timeSeriesTable = new TimeSeriesTable(theDb);
		timeSeriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		myscrollpane.add(timeSeriesTable);
		myscrollpane.setViewportView(timeSeriesTable);
		myscrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		return myscrollpane;
	}

	private JPanel closePanel()
	{
		JPanel closePanel = new JPanel(new FlowLayout());
		JButton closeButton = new JButton();
		closeButton.setText(closeButtonLabel);
		closeButton.setPreferredSize(new java.awt.Dimension(100, 25));
		closeButton.setName("closeButton");
		closeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				doClose();
			}
		});
		closePanel.add(closeButton);
		return closePanel;
	}

	private boolean doClose()
	{ // depending on the mode that this GUI was started, we'll call
		// System exit or not
		if ((myoutputs != null) && (myoutputs.size() != 0) && needToSave)
		{
			int r = JOptionPane.showConfirmDialog(this, saveCompOutput);
			if (r == JOptionPane.CANCEL_OPTION)
				return false;
			else if (r == JOptionPane.YES_OPTION)
				saveCompOutput();
			else
				needToSave = false;
		}
		if (!standAloneMode)
		{
			compEditParent.setRunCompGUIUp(false);
			dispose();
		}
		else
		{
			dispose();
			if (exitOnClose)
				System.exit(0);
		}
		return true;
	}

	/**
	 * This is used when this GUI is launch from the Comp Edit GUI
	 * 
	 * @return true or false
	 */
	public boolean closeFromParent()
	{
		return doClose();
	}

	private void saveButtonPressed()
	{
		if ((myoutputs == null) || (myoutputs.size() == 0))
		{
			JOptionPane.showMessageDialog(this, "No computation output !",
				labels.getString("RunComputationsFrame.frameTitle"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		int r = JOptionPane.showConfirmDialog(this, "Are you sure to save the computation output ?",
			labels.getString("RunComputationsFrame.frameTitle"), JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
		{
			saveCompOutput();
			needToSave = false;
		}

	}

	private void saveCompOutput()
	{
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		try
		{
			for (CTimeSeries myseries : myoutputs)
			{
				try
				{
					Logger.instance().info("Calling saveTimeSeries, size=" + myseries.size());
					timeSeriesDAO.saveTimeSeries(myseries);
				}
				catch (DbIoException ex)
				{
					String msg = module + " Can not write Time Series to the " + "Database "
						+ ex.getMessage();
					showError(msg);
				}
				catch (BadTimeSeriesException ex)
				{
					String msg = module + " Can not write Time Series to the " + "Database "
						+ ex.getMessage();
					Logger.instance().failure(msg);
				}
			}
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	private void selectButtonPressed()
	{
		if (theDb != null)
		{
			Vector<DbComputation> myvector = new Vector<DbComputation>();

			if (computationsListDialog == null)
			{
				computationsListDialog = new ComputationsListDialog(this, theDb, getRunCompFrametester());
			}

			launchDialog(computationsListDialog);
			myvector = computationsListDialog.getComputations();

			if (computationsListDialog.wasOK())
			{
				mytable.fill(myvector);
			}
		}
	}

	private void removeButtonPressed()
	{
		mytable.remove();
	}

	/** For testing purpose */
	public static void main(String[] args)
	{
		JFrame myframe = new CompRunGuiFrame(true);
		myframe.setVisible(true);
		myframe.setSize(400, 400);
	}

	/**
	 * @return the runCompFrametester
	 */
	public RunComputationsFrameTester getRunCompFrametester()
	{
		return runCompFrametester;
	}

	/**
	 * @param runCompFrametester
	 *            the runCompFrametester to set
	 */
	public void setRunCompFrametester(RunComputationsFrameTester runCompFrametester)
	{
		this.runCompFrametester = runCompFrametester;

	}
}

class ComputationsTable extends JTable
{
	ComputationsTableModel mymodel = new ComputationsTableModel();

	ComputationsTable()
	{
		this.setModel(mymodel);
	}

	public String getColumnName(int column)
	{
		if (column == 0)
		{
			return CompRunGuiFrame.labels.getString("RunComputationsFrame.computationName");
		}
		else if (column == 1)
		{
			return CompRunGuiFrame.description;
		}
		else
		{
			return "";
		}
	}

	public void fill(Vector<DbComputation> newvector)
	{
		mymodel.fill(newvector);
	}

	public void remove()
	{
		int[] rows = this.getSelectedRows();
		for (int pos = 0; pos < rows.length; pos++)
		{
			mymodel.remove(rows[pos]);
		}
	}

}

class ComputationsTableModel extends AbstractTableModel
{
	public Vector<DbComputation> myvector = new Vector<DbComputation>();

	public String getColumnName(int column)
	{
		if (column == 0)
		{
			return CompRunGuiFrame.labels.getString("RunComputationsFrame.computationName");
		}
		else if (column == 1)
		{
			return CompRunGuiFrame.description;
		}
		else
		{
			return "";
		}
	}

	public Object getValueAt(int y, int x)
	{
		if (x == 0)
		{
			return myvector.get(y).getName();
		}
		else if (x == 1)
		{
			return myvector.get(y).getComment();
		}
		else
		{
			return null;
		}
	}

	public int getColumnCount()
	{
		return 2;
	}

	public int getRowCount()
	{
		return myvector.size();
	}

	public void remove(int row)
	{
		if (row >= 0 && row < myvector.size())
		{
			myvector.remove(row);
			this.fireTableDataChanged();
		}
	}

	public void fill(Vector<DbComputation> newvector)
	{
		myvector.addAll(newvector);
		// myvector = newvector;
		this.fireTableDataChanged();
	}
}

class ComputationsListDialog extends JDialog
{
	private ComputationsListPanel compListPanel;
	private boolean ok = false;
	private RunComputationsFrameTester mytester;

	ComputationsListDialog(TopFrame owner, TimeSeriesDb mydb, RunComputationsFrameTester frameTester)
	{
		super(owner, CompRunGuiFrame.selectFromListLabel, true);
		this.mytester = frameTester;

		boolean noCompToken = mytester.getNoCompFilterToken();
		compListPanel = new ComputationsListPanel(mydb, !noCompToken, true, owner);

		compListPanel.setIsDialog(true);
		this.setLayout(new BorderLayout());
		this.add(compListPanel, BorderLayout.CENTER);
		this.setSize(750, 600);
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton(CompRunGuiFrame.okButtonLabel);// "  OK  "
		JButton cancelButton = new JButton(CompRunGuiFrame.cancelButtonLabel);
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonPressed();
			}
		});

		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonPressed();
			}
		});

		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		this.add(buttonPanel, BorderLayout.SOUTH);

	}

	/**
	 * @return the mytester
	 */
	public RunComputationsFrameTester getMytester()
	{
		return mytester;
	}

	/**
	 * @param mytester
	 *            the mytester to set
	 */
	public void setMytester(RunComputationsFrameTester mytester)
	{
		this.mytester = mytester;
	}

	private void okButtonPressed()
	{
		ok = true;
		setVisible(false);
		dispose();
	}

	private void cancelButtonPressed()
	{
		setVisible(false);
		dispose();
	}

	public boolean wasOK()
	{
		return ok;
	}

	public Vector<DbComputation> getComputations()
	{
		if (ok)
		{
			return compListPanel.getSelectedComputations();
		}
		else
		{
			return new Vector<DbComputation>();
		}
	}
}