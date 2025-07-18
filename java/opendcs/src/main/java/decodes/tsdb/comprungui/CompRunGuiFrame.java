/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2018/11/14 15:59:56  mmaloney
 * Remove obsolete catch block.
 *
 * Revision 1.2  2018/06/04 19:23:38  mmaloney
 * HDB issue where deleted values were being displayed on table and graph.
 *
 * Revision 1.1  2017/08/22 19:57:35  mmaloney
 * Refactor
 *
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
import java.beans.PropertyChangeEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import opendcs.dai.AlgorithmDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;

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
import org.opendcs.gui.tables.DateRenderer;
import org.slf4j.LoggerFactory;

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
import decodes.tsdb.alarm.AlarmManager;
import decodes.tsdb.compedit.ComputationsEditPanel;
import decodes.tsdb.compedit.ComputationsListPanel;
import decodes.tsdb.comprungui.TimeSeriesTableModel.ColumnInfo;
import decodes.tsdb.groupedit.TimeSeriesSelectDialog;
import decodes.util.DecodesSettings;

/**
 * The frame for running computations interactively.
 */
@SuppressWarnings("serial")
public class CompRunGuiFrame extends TopFrame
{
	private static org.slf4j.Logger log = LoggerFactory.getLogger(CompRunGuiFrame.class);
	public static final ResourceBundle labels = RunComputationsFrameTester.getLabels();;
	private static final ResourceBundle genericLabels = RunComputationsFrameTester.getGenericLabels();;
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
	private String cancelComputationExecution;
	public static String okButtonLabel;// generic
	public static String cancelButtonLabel;// generic
	public static String dateTimeColumnLabel = labels.getString("TimeSeriesTable.dateTimeColumnLabel") + " (UTC)";
	public static final String inputLabel = labels.getString("RunComputationsFrame.inputLabel");
	public static final String outputLabel = labels.getString("RunComputationsFrame.outputLabel");;

	private ComputationsTable mytable;
	private Vector<CTimeSeries> myoutputs = new Vector<>();
	private Vector<CTimeSeries> myinputs = new Vector<>();
	private TimeSeriesDb theDb = null;
	private DateTimeCalendar fromDTCal;
	private DateTimeCalendar toDTCal;
	private TimeSeriesCollection[] datasets;
	private JFreeChart mychart;
	private TimeSeriesTablePanel timeSeriesTablePanel = new TimeSeriesTablePanel();
	private String module = "RunComputationFrame";
	private ChartPanel chartPanel;

	private boolean standAloneMode = false;
	private boolean needToSave = false;
	private ComputationsEditPanel compEditParent;
	private String timeZoneStr;
	private RunComputationsFrameTester runCompFrametester;
	private ComputationsListDialog computationsListDialog = null;
	private JButton traceButton = new JButton("Trace Execution");
	private JProgressBar progressBar = new JProgressBar(0,100);
	private SwingWorker<List<CTimeSeries>,CTimeSeries> compExecutionWorker = null;
	private SwingWorker<Vector<DbComputation>,Void> buildTimeSeriesListWorker = null;
	private JButton cancelExecutionButton = null;
	private JButton runButton = null;
	JButton saveButton = null;

	private TraceDialog traceDialog = null;
	private String cancelComputationExecutionLabel;
	private ProgressState progress;
	/**
	 * Constructor
	 * 
	 * @param standAloneMode
	 *            True if running from launcher or tester. False if running
	 *            inside compedit.
	 */
	public CompRunGuiFrame(boolean standAloneMode)
	{
		this(standAloneMode, null, null, null);
	}

	/**
	 * Constructor
	 *
	 * @param standAloneMode
	 *            True if running from launcher or tester. False if running
	 *            inside compedit.
	 * @param dbComps
	 * 	          List of comps to fill computation table initially.
	 * @param since
	 * 	          From Date to fill fromDTcal initially.
	 * @param until
	 * 	          To Date to fill toDTcal initially.
	 */
	public CompRunGuiFrame(boolean standAloneMode, Vector<DbComputation> dbComps, Date since, Date until)
	{
		super();

		this.standAloneMode = standAloneMode;

		
		timeZoneStr = DecodesSettings.instance().sqlTimeZone;
		timeZoneStr = timeZoneStr == null ? "UTC" : timeZoneStr;
		setAllLabels();
		chartXLabel = "Time";

		JPanel mycontent = (JPanel)this.getContentPane();
		mycontent.setLayout(new BoxLayout(mycontent, BoxLayout.Y_AXIS));

		this.setTitle(labels.getString("RunComputationsFrame.frameTitle"));
		this.trackChanges("runcomps");
		traceDialog = new TraceDialog(this, false);
		traceDialog.setTraceType("Computation Run");
		mycontent.add(listPanel());
		mycontent.add(timePanel());
		mycontent.add(getChart());
		mycontent.add(timeSeriesTablePanel);
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

		if(since!=null){
			fromDTCal.setDate(since);
		}
		if(until!=null) {
			toDTCal.setDate(until);
		}

		if (dbComps != null)
		{
			mytable.fill(dbComps);
		}


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
		cancelComputationExecution = labels.getString("RunComputationsFrame.cancelComputationExecution");
		okButtonLabel = genericLabels.getString("OK");
		cancelButtonLabel = genericLabels.getString("cancel");
		cancelComputationExecutionLabel = cancelButtonLabel;
		dateTimeColumnLabel = labels.getString("TimeSeriesTable.dateTimeColumnLabel") + " (" + timeZoneStr
			+ ")";
		
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
		GridBagLayout gbl_time = new GridBagLayout();
		gbl_time.columnWidths = new int[]{353, 150, 345, 0};
		gbl_time.rowHeights = new int[]{76, 0};
		gbl_time.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_time.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		time.setLayout(gbl_time);

		JPanel runhalf = new JPanel();
		runhalf.setLayout(new GridBagLayout());
		runButton = new JButton(runCompsButton);
		runButton.addActionListener(e -> runButtonPressed());

		saveButton = new JButton(saveOutputButton);
		saveButton.setEnabled(false);
		saveButton.addActionListener(e -> saveButtonPressed());

		traceButton.setEnabled(true);
		traceButton.addActionListener(e -> traceButtonPressed());

		Date tempDate = new Date();
		fromDTCal = new DateTimeCalendar(fromLabel, null, "dd MMM yyyy", timeZoneStr);
		toDTCal = new DateTimeCalendar(toLabel, tempDate, "dd MMM yyyy", timeZoneStr);
		JPanel timehalf = new JPanel();
		timehalf.setLayout(new GridBagLayout());
		timehalf.add(fromDTCal, new GridBagConstraints(0, 0, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(new JLabel(" (" + timeZoneStr + ")"), new GridBagConstraints(1, 0, 1, 1, 0, 0,
			GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(4, 10, 4, 10), 0, 0));
		timehalf.add(toDTCal, new GridBagConstraints(0, 1, 1, 1, 0.5, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 4, 10), 0, 0));
		GridBagConstraints gbc_timehalf = new GridBagConstraints();
		gbc_timehalf.anchor = GridBagConstraints.NORTHWEST;
		gbc_timehalf.insets = new Insets(0, 0, 0, 5);
		gbc_timehalf.gridx = 0;
		gbc_timehalf.gridy = 0;
		time.add(timehalf, gbc_timehalf);
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
		gbc_progressBar.insets = new Insets(0, 10, 0, 10);
		gbc_progressBar.gridx = 1;
		gbc_progressBar.gridy = 0;
		time.add(progressBar, gbc_progressBar);

		runhalf.add(runButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 5, 10), 0, 0));
		runhalf.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 5, 10), 0, 0));
		runhalf.add(traceButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(4, 10, 0, 10), 0, 0));
		GridBagConstraints gbc_runhalf = new GridBagConstraints();
		gbc_runhalf.anchor = GridBagConstraints.EAST;
		gbc_runhalf.fill = GridBagConstraints.VERTICAL;
		gbc_runhalf.gridx = 2;
		gbc_runhalf.gridy = 0;
		time.add(runhalf, gbc_runhalf);
		
		cancelExecutionButton = new JButton(cancelComputationExecutionLabel);
		cancelExecutionButton.setEnabled(false);
		GridBagConstraints gbc_cancelExecutionButton = new GridBagConstraints();
		gbc_cancelExecutionButton.insets = new Insets(4, 10, 5, 10);
		gbc_cancelExecutionButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_cancelExecutionButton.gridx = 1;
		gbc_cancelExecutionButton.gridy = 1;
		runhalf.add(cancelExecutionButton, gbc_cancelExecutionButton);
		cancelExecutionButton.addActionListener(e ->
		{
			if (this.compExecutionWorker != null && !this.compExecutionWorker.isDone())
			{				
				this.compExecutionWorker.cancel(true);
				this.cancelExecutionButton.setEnabled(false);
			}
			else if (this.buildTimeSeriesListWorker != null && !this.buildTimeSeriesListWorker.isDone())
			{
				this.buildTimeSeriesListWorker.cancel(true);
				this.cancelExecutionButton.setEnabled(false);
			}
		});
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
			
			// Don't plot values flagged for deletion.
			if (tv == null || VarFlags.mustDelete(tv))
				continue;
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
		TimeSeriesTableModel tsTm = timeSeriesTablePanel.getTimeSeriesModel();
		if (theDb != null)
		{
			tsTm.setRevColumnInfo(new ColumnInfo(theDb.getRevisionLabel(), tv -> theDb.flags2RevisionCodes(tv.getFlags())));
			tsTm.setLimitColumnInfo(new ColumnInfo(theDb.getLimitLabel(), tv -> theDb.flags2LimitCodes(tv.getFlags())));
		}
		else
		{
			tsTm.setRevColumnInfo(new ColumnInfo("", tv -> null));
			tsTm.setLimitColumnInfo(new ColumnInfo("", tv -> null));
		}
	}

	private void runButtonPressed()
	{
		// Create a trace logger and put in the pipeline with tee logger.
		Logger originalLogger = Logger.instance();
		final TraceLogger traceLogger = new TraceLogger(originalLogger.getProcName());
		final TeeLogger teeLogger = new TeeLogger(originalLogger.getProcName(), originalLogger, traceLogger);
		traceLogger.setMinLogPriority(Logger.E_DEBUG3);
		Logger.setLogger(teeLogger);

		runButton.setEnabled(false);
		traceDialog.clear();
		traceLogger.setDialog(traceDialog);
		AlarmManager.deleteInstance();
		
		if (fromDTCal.getDate() == null || toDTCal.getDate() == null)
		{
			return;
		}
		final Vector<CTimeSeries> inputs = new Vector<CTimeSeries>();
		final Vector<DbComputation> compVector = new Vector<DbComputation>();

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

		ifSelectionValid(compVector, (comps,compGroup) ->
			{
			progressBar.setStringPainted(true);
			progressBar.setValue(0);
			buildTimeSeriesListWorker = new SwingWorker<Vector<DbComputation>,Void>()
			{

				@Override
				protected Vector<DbComputation> doInBackground() throws Exception
				{
					if (!buildComputationList(compVector, compGroup, (progress) -> setProgress(progress), () -> isCancelled()))
					{
						return null;
					}
					else
					{
						return compVector;
					}
				}

				@Override
				public void done()
				{
					try
					{
						if (this.isCancelled())
						{
							runButton.setEnabled(true);
							return;
						}
						setProgress(100);
						Vector<DbComputation> theComps = this.get();
						if (theComps != null)
						{
							runSelectedComps(theComps, originalLogger, traceLogger, inputs);
						}
						else
						{
							runButton.setEnabled(true);
						}
					}
					catch (InterruptedException | ExecutionException ex)
					{
						runButton.setEnabled(true);
						log.atError()
						.setCause(ex)
						.log("Unable to execute computations.");
						showError(ex.getLocalizedMessage());
					}
				}
			};
			buildTimeSeriesListWorker.addPropertyChangeListener(event -> updateProgress(event));
			buildTimeSeriesListWorker.execute();
		});
	}

	/**
	 * Determines if the user selection is valid, if so pass on to next step, otherwise just return.
	 * @param compVector
	 * @param actionIfValid contains the code to run if the selection is valid.
	 */
	private void ifSelectionValid(Collection<DbComputation> compVector, BiConsumer<Collection<DbComputation>,TsGroup> actionIfValid)
	{
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
					compGroup = new TsGroup();
					compGroup.clear();
					break;
				}
				else
				{
					Logger.instance().debug3(
						"comp " + comp.getName() + " also uses group id=" + comp.getGroupId());
				}
			}
		}
		if (compGroup == null || (compGroup.getGroupName() != null))
		{
			actionIfValid.accept(compVector, compGroup);
		}
		else
		{
			log.trace("Requested computations use a different group. Unable to process.");
		}
	}

	/**
	 * Generates a list of fully concrete (algorithm + all inputs/outputs fully expands)
	 * @param compVector vector instance that will hold the generated computations.
	 * @param compGroup The Timeseries Group getting used for this computation. Maybe null to indicate a group comp
	 *                  isn't being run and inputs and outputs are already defined.
	 * @param setProgress Function that accepts the current progress of timeseries mutation
	 * @return true if the expansion of the compVector was successful.
	 */
	private boolean buildComputationList(Vector<DbComputation> compVector, TsGroup compGroup,
										 Consumer<Integer> setProgress, Supplier<Boolean> checkCancelled)
	{
		ArrayList<TimeSeriesIdentifier> groupTsIds = new ArrayList<TimeSeriesIdentifier>();
		if (compGroup != null)
		{
			try
			{
				cancelExecutionButton.setEnabled(true);
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
					return false;
				}

				// Read a fresh copy of the group from the DB
				
				try (TsGroupDAI groupDAO = theDb.makeTsGroupDAO())
				{
					compGroup = groupDAO.getTsGroupById(compGroup.getGroupId());
				}

				// Expand the group and then filter it by the 1st input parm.
				ArrayList<TimeSeriesIdentifier> tsids = theDb.expandTsGroup(compGroup);
				progress = new ProgressState(tsids.size());
				// Collect the transformed tsids in a hash set to remove any
				// duplicates
				// that may result by transformation.
				final TreeSet<TimeSeriesIdentifier> transformedTsids = new TreeSet<TimeSeriesIdentifier>();
				final DbCompParm theFirstInput = firstInput;
				try (TimeSeriesDAI txDAI = theDb.makeTimeSeriesDAO())
				{
					txDAI.inTransaction(dao ->
					{
						try (TimeSeriesDAI tsDAI = theDb.makeTimeSeriesDAO())
						{
							tsDAI.inTransactionOf(dao);
							for (Iterator<TimeSeriesIdentifier> tsidit = tsids.iterator(); tsidit.hasNext();)
							{
								if (checkCancelled.get() == true)
								{
									throw new Exception("stop");
								}
								TimeSeriesIdentifier tsid = tsidit.next();

								// Transform the tsid by the parm. If the result is the same
								// as the original, that means that the parm 'matches'.
								TimeSeriesIdentifier transformed;
								progress.incDone();
								setProgress.accept(progress.getPercentDone());
								try
								{
									transformed = theDb.transformTsidByCompParm(tsDAI, tsid, theFirstInput, false, false, "");
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
						}
					});
				}
				catch (Exception ex)
				{
					if ("stop".equals(ex.getMessage()))
					{
						return false;
					}
					throw new DbIoException("Error processing timeseries group.", ex);
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
				{
					groupTsIds.add(tsid);
				}

				Logger.instance().debug3("Will execute with the following time-series: ");
				for (TimeSeriesIdentifier tsid : groupTsIds)
				{
					Logger.instance().debug3("   " + tsid.getUniqueString());
				}

				ArrayList<DbComputation> concreteGroupComps = new ArrayList<DbComputation>();
				StringBuilder errorMsgs = new StringBuilder();
				try (TimeSeriesDAI txDAI = theDb.makeTimeSeriesDAO())
				{
					txDAI.inTransaction(dao ->
					{
						try (TimeSeriesDAI inTxDai = theDb.makeTimeSeriesDAO())
						{
							inTxDai.inTransactionOf(dao);
							for (Iterator<DbComputation> compit = compVector.iterator(); compit.hasNext();)
							{
								DbComputation localComp = compit.next();
								if (!localComp.hasGroupInput())
								{
									continue;
								}

								// This is a group computation. Use resolver to make
								// a concrete copy, and then add it to concreteGroupComps.
								// First call compit.remote() to remove the abstract copy.
								compit.remove();

								for (TimeSeriesIdentifier tsid : groupTsIds)
								{
									try
									{
										DbComputation concrete = DbCompResolver.makeConcrete(theDb, txDAI, tsid, localComp, true);
										concreteGroupComps.add(concrete);
									}
									catch (NoSuchObjectException ex)
									{
										errorMsgs.append("Cannot execute '" + comp.getName() + "' for time series '"
											+ tsid.getUniqueString() + "': " + ex.getMessage() + "\r\n");
									}
								}
							}
						}
					});
				}
				catch (Exception ex)
				{
					throw new DbIoException("Unable to create concrete group comps.", ex);
				}
				compVector.addAll(concreteGroupComps);
			}
			catch (DbIoException ex)
			{
				showError("Cannot expand group '" + compGroup.getGroupName() + "': " + ex);
				return false;
			}
		}
		return true;
	}

	/**
	 * Actually run the selected computation with the given inputs.
	 * @param compVector list of computations that are fully defined. E.g. it is explicit *what* timeseries to use for input and output.
	 * @param originalLogger
	 * @param traceLogger
	 * @param inputs valid vector that is used to add input timeseries for computations that are actually run.
	 */
	private void runSelectedComps(Collection<DbComputation> compVector, Logger originalLogger, TraceLogger traceLogger, Vector<CTimeSeries> inputs)
	{
		final Vector<CTimeSeries> both = new Vector<CTimeSeries>();
		// Flush the text area inside trace dialog
		// Create the trace logger here and put in pipe with tee logger.
		// Put trace dialog reference in trace logger.
		myoutputs.clear();
		myinputs.clear();
		myinputs.addAll(inputs);
		compExecutionWorker = new SwingWorker<List<CTimeSeries>,CTimeSeries>() {
			@Override
			public List<CTimeSeries> doInBackground()
			{
				runButton.setEnabled(false);
				Vector<CTimeSeries> outputs = new Vector<CTimeSeries>();
				progress = new ProgressState(compVector.size());
				for (DbComputation comp : compVector)
				{
					if(this.isCancelled())
					{
						return outputs;
					}
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
								if (!isCancelled())
								{
									showError(msg);
								}
								Logger.instance().warning(msg);
								StringWriter sw = new StringWriter();
								PrintWriter pw = new PrintWriter(sw);
								ex.printStackTrace(pw);
								Logger.instance().warning(sw.toString());
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
								myinputs.add(ts);
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
					try (AlgorithmDAI algoDAO = theDb.makeAlgorithmDAO())
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
						comp.setAlgorithm(algoDAO.getAlgorithmById(comp.getAlgorithmId()));
						comp.prepareForExec(theDb);
						comp.apply(runme, theDb);
					}
					catch (DbCompException e)
					{
						if (!isCancelled())
						{
							showError(module + " DbCompException in " + "runButtonPressed() " + e.getMessage());
						}
						continue;
					}
					catch (DbIoException e)
					{
						if (!compExecutionWorker.isCancelled())
						{
							showError(module + " DbIOException in " + "runButtonPressed() " + e.getMessage());
						}
						continue;
					}
					catch (NoSuchObjectException e)
					{
						if (!compExecutionWorker.isCancelled())
						{
							showError(module + " Cannot read Algorithm in " + "runButtonPressed() " + e.getMessage());
						}
						continue;
					}
					progress.incDone();
					setProgress(progress.getPercentDone());
					// Get all outputs & add outputs to total lists;
					for (DbCompParm parm : outputParms)
					{
						boolean found = false;
						for (CTimeSeries cts : runme.getAllTimeSeries())
							if (cts.getSDI() == parm.getSiteDataTypeId()
								&& TextUtil.strEqual(cts.getInterval(), parm.getInterval())
								&& TextUtil.strEqual(cts.getTableSelector(), parm.getTableSelector()))
							{
								publish(cts);
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
				return outputs;
			}

			@Override
			protected void process(List<CTimeSeries> chunks)
			{
				for (CTimeSeries cts : myinputs)
				{
					if (!both.contains(cts))
					{
						both.add(cts);
					}
				}

				for (CTimeSeries cts : chunks)
				{
					if (both.contains(cts))
						continue;

					myoutputs.add(cts);

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
					plotDataOnChart(both, myinputs.size());
					timeSeriesTablePanel.getTimeSeriesModel().setInOut(myinputs, myoutputs);
				}
			}

			@Override
			protected void done()
			{
				runButton.setEnabled(true);
				setProgress(100);
				// Stop trace logger and remove from pipeline
				traceLogger.setDialog(null);
				Logger.setLogger(originalLogger);
				cancelExecutionButton.setEnabled(false);
				saveButton.setEnabled(true);
				plotDataOnChart(both, myinputs.size());
				timeSeriesTablePanel.getTimeSeriesModel().setInOut(myinputs, myoutputs);
			}
		};
		compExecutionWorker.addPropertyChangeListener(event -> updateProgress(event));
		progressBar.setStringPainted(true);
		progressBar.setString("Running");
		progressBar.setValue(0);
		saveButton.setEnabled(false);
		cancelExecutionButton.setEnabled(true);
		compExecutionWorker.execute();
		needToSave = true;
	}

	private void updateProgress(PropertyChangeEvent event)
	{
		if ("progress".equals(event.getPropertyName()))
		{
			int value = (Integer)event.getNewValue();
			progressBar.setValue(value);
			if (value == 100)
			{
				progressBar.setString("done");
			}
			else if (progress != null)
			{
				progressBar.setString(String.format("%d of %d", progress.getDone(), progress.getTotal()));
			}
		}
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
	{
		if (compExecutionWorker != null && !compExecutionWorker.isDone())
		{
			int r = JOptionPane.showConfirmDialog(this, cancelComputationExecution);
			if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.NO_OPTION)
			{
				return false;
			}
			else
			{
				compExecutionWorker.cancel(true);
				needToSave = false;
			}
		}
		if ((myoutputs != null) && (myoutputs.size() != 0) && needToSave)
		{
			int r = JOptionPane.showConfirmDialog(this, saveCompOutput);
			switch(r)
			{
				case JOptionPane.CANCEL_OPTION:
				{
					return false;
				}
				case JOptionPane.YES_OPTION:
				{
					saveCompOutput();
					break;
				}
				default:
				{
					needToSave = false;
				}
			}
		}
		// depending on the mode that this GUI was started, we'll call
		// System exit or not
		if (!standAloneMode)
		{
			compEditParent.setRunCompGUIUp(false);
			dispose();
		}
		else
		{
			dispose();
			if (exitOnClose)
			{
				/**
				 * TODO: This shouldn't be necassary and Java will exit when the last non-daemon
				 *  thread exits and we should rely on that behavior instead of forcing a System.exit
				 */ 
				System.exit(0);
			}
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
			timeSeriesTablePanel.getTimeSeriesModel().setInOut(myinputs, myoutputs);
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

	private static class ProgressState
	{
		private int total;
		private int done;

		public ProgressState(int total)
		{
			this.total = total;
			this.done = 0;
		}

		public void incDone()
		{
			done++;
		}

		public int getTotal()
		{
			return total;
		}

		public int getDone()
		{
			return done;
		}

		public int getPercentDone()
		{
			return (100*done)/total;
		}
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