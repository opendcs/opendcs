/*
* $Id$
*/
package decodes.decwiz;

import java.awt.*;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.border.Border;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;

import ilex.util.Logger;
import ilex.util.TeeLogger;
import ilex.var.TimedVariable;
import decodes.db.*;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.consumer.StringBufferConsumer;
import decodes.datasource.UnknownPlatformException;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;
import decodes.decoder.SummaryReportGenerator;
import decodes.decoder.TimeSeries;
import decodes.dbeditor.TraceDialog;
import decodes.dbeditor.TraceLogger;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

public class DecodePanel 
	extends DecWizPanel
{
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel northPanel = new JPanel();
	private JButton startDecodeButton = new JButton();
	private GridBagLayout northLayout = new GridBagLayout();
	private JPanel timeShiftPanel = new JPanel();
	private JLabel fromLabel = new JLabel();
	private JTextField fromField = new JTextField();
	private JLabel toLabel = new JLabel();
	private JTextField toField = new JTextField();
	private JLabel shiftLabel = new JLabel();
	private JTextField shiftField = new JTextField();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private TitledBorder titledBorder1 = new TitledBorder("");
	private Border border1 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border2 = new TitledBorder(border1, "Time Shift");
	private JPanel centerPanel = new JPanel();
	private JPanel decodedDataPanel = new JPanel();
	private TitledBorder titledBorder2 = new TitledBorder("");
	private Border border3 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border4 = new TitledBorder(border3, "Decoded Data");
	private JScrollPane decodedDataScrollPane = new JScrollPane();
	private BorderLayout borderLayout2 = new BorderLayout();
	private JTextArea decodedDataArea = new JTextArea();
	private TitledBorder titledBorder3 = new TitledBorder("");
	private Border border5 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border6 = new TitledBorder(border5, "Decoding Log");
	private JPanel summaryPanel = new JPanel();
	private BorderLayout borderLayout4 = new BorderLayout();
	private TitledBorder titledBorder4 = new TitledBorder("");
	private Border border7 = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border border8 = new TitledBorder(border7, "Decoding Summary");
	private JScrollPane summaryScrollPane = new JScrollPane();
	private JTextArea summaryArea = new JTextArea();
	private JSplitPane jSplitPane1 = new JSplitPane();
	private BorderLayout borderLayout3 = new BorderLayout();
	private JButton traceLogButton = new JButton();
	private JCheckBox timeShiftCheck = new JCheckBox("Do Time Shift");
	private JLabel maxMissingLabel = new JLabel("Max Missing:");
	private JTextField maxMissingField = new JTextField();

	//=======================================================
	private DecodedMessage decodedMessage = null;
	private TraceDialog traceDialog = null;
	private PresentationGroup presentationGroup = null;
	private SummaryReportGenerator sumRepGen = null;
	private String shiftDatePatterns[] = 
		{ "yyyy/MM/dd HH:mm", "MM/dd HH:mm", "HH:mm",
		  "yyyy/MM/dd HH:mm:ss", "MM/dd HH:mm:ss", "HH:mm:ss" };
	private SimpleDateFormat dateFormat = 
		new SimpleDateFormat("yyyy/MM/dd HH:mm");
	private Date fromDate = null;
	private Date toDate = null;
	private long shiftValue = 0L;
	private int maxMissing = 4;

	/** Constructor. */
	public DecodePanel()
	{
		super();
		try
		{
			jbInit();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
		sumRepGen = new SummaryReportGenerator();
		String tzs = DecodesSettings.instance().decwizTimeZone;
		sumRepGen.setTimeZone(tzs);
		timeShiftCheckPressed();
		dateFormat.setTimeZone(TimeZone.getTimeZone(tzs));
	}

	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		startDecodeButton.setPreferredSize(new Dimension(140, 27));
		startDecodeButton.setText("Decode Data");
		startDecodeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				startDecodePressed();
			}
		});
		northPanel.setLayout(northLayout);
		fromLabel.setText("From: ");
		fromField.setPreferredSize(new Dimension(120, 23));
		fromField.setToolTipText("YYYY/MM/DD HH:MM - start time of shift");
		fromField.setText("");
		toLabel.setText("     To: ");
		toField.setPreferredSize(new Dimension(120, 23));
		toField.setToolTipText("YYYY/MM/DD HH:MM - end time of shift");
		toField.setText("");
		shiftLabel.setText("     Shift: ");
		shiftField.setPreferredSize(new Dimension(100, 23));
		shiftField.setToolTipText("Time shift: +/- HH:MM:SS");
		shiftField.setText("");
		timeShiftCheck.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				timeShiftCheckPressed();
			}
		});
		timeShiftPanel.setLayout(gridBagLayout1);
		timeShiftPanel.setBorder(border2);
		centerPanel.setLayout(borderLayout3);
		decodedDataPanel.setBorder(border4);
		decodedDataPanel.setLayout(borderLayout2);
		decodedDataPanel.setPreferredSize(new Dimension(196, 140));
		Font oldfont = decodedDataArea.getFont();
		decodedDataArea.setFont(
			new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		summaryPanel.setLayout(borderLayout4);
		summaryPanel.setBorder(border8);
		summaryPanel.setPreferredSize(new Dimension(161, 40));
		summaryArea.setFont(
			new Font("Monospaced", Font.PLAIN, oldfont.getSize()));
		jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		traceLogButton.setEnabled(false);
		traceLogButton.setPreferredSize(new Dimension(140, 27));
		traceLogButton.setText("Trace Log");
		traceLogButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				traceLogButtonPressed();
			}
		});
		this.add(northPanel, java.awt.BorderLayout.NORTH);
		northPanel.add(startDecodeButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));

		northPanel.add(traceLogButton,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));

		northPanel.add(maxMissingLabel,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 2), 0, 0));
		northPanel.add(maxMissingField,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 0, 0));
		maxMissingField.setPreferredSize(new Dimension(80, 23));
		maxMissingField.setText("" + maxMissing);

		this.add(timeShiftPanel, java.awt.BorderLayout.SOUTH);

		timeShiftPanel.add(timeShiftCheck,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 2), 0, 0));
		timeShiftPanel.add(fromLabel,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 2), 0, 0));
		timeShiftPanel.add(fromField,
			new GridBagConstraints(2, 0, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 0), 0, 0));
		timeShiftPanel.add(toLabel, 
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 2), 0, 0));
		timeShiftPanel.add(toField, 
			new GridBagConstraints(4, 0, 1, 1, 0.4, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 0), 0, 0));
		timeShiftPanel.add(shiftLabel,
			new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 2), 0, 0));
		timeShiftPanel.add(shiftField,
			new GridBagConstraints(6, 0, 1, 1, 0.2, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 0, 0));

		this.add(centerPanel, java.awt.BorderLayout.CENTER);
		decodedDataPanel.add(decodedDataScrollPane,
							 java.awt.BorderLayout.CENTER);
		centerPanel.add(jSplitPane1, java.awt.BorderLayout.CENTER);
		jSplitPane1.setTopComponent(decodedDataPanel);
		jSplitPane1.setBottomComponent(summaryPanel);
		summaryPanel.add(summaryScrollPane, java.awt.BorderLayout.CENTER);
		summaryScrollPane.getViewport().add(summaryArea);
		decodedDataScrollPane.getViewport().add(decodedDataArea);
		jSplitPane1.setDividerLocation(180);
	}

	/**
	 * Get the shift info from the GUI controls.
	 * @return true if OK to proceed with decode.
	 */
	private boolean getShiftInfo()
	{
		if (timeShiftCheck.isSelected())
		{
			if (!fromFieldEntered()
			 || !toFieldEntered()
			 || !shiftFieldEntered())
				return false;
		}
		return true;
	}

	public boolean shiftFieldEntered()
	{
		int sign = 1;
		String s = shiftField.getText().trim();
		shiftValue = 0L;
		if (s.length() == 0)
			return true;
		int idx = 0;
		char c = s.charAt(idx);
		if (c == '-')
		{
			sign = -1;
			idx++;
		}
		else if (c == '+')
			idx++;
		while(idx < s.length() && Character.isWhitespace(c = s.charAt(idx)))
			idx++;
		if (idx >= s.length())
			return true;
		StringTokenizer st = new StringTokenizer(s.substring(idx),":");
		if (st.hasMoreTokens())
		{
			String ts = st.nextToken();
			try
			{
				int h = Integer.parseInt(ts);
				shiftValue = h * 3600000L;
			}
			catch(NumberFormatException ex)
			{
				showError("Invalid shift. Enter 'HH:MM:SS' or '-HH:MM:SS'.");
				return false;
			}
		}
		if (st.hasMoreTokens())
		{
			String ts = st.nextToken();
			try
			{
				int m = Integer.parseInt(ts);
				shiftValue += (m * 60000L);
			}
			catch(NumberFormatException ex)
			{
				showError("Invalid shift. Enter 'HH:MM:SS' or '-HH:MM:SS'.");
				shiftValue = 0L;
				return false;
			}
		}
		if (st.hasMoreTokens())
		{
			String ts = st.nextToken();
			try
			{
				int i = Integer.parseInt(ts);
				shiftValue += (i * 1000L);
			}
			catch(NumberFormatException ex)
			{
				showError("Invalid shift. Enter 'HH:MM:SS' or '-HH:MM:SS'.");
				shiftValue = 0L;
				return false;
			}
		}
		shiftValue *= sign;
		return true;
	}

	public boolean fromFieldEntered()
	{
		String ds = fromField.getText().trim();
		fromDate = null;
		if (ds.length() == 0)
			return true;
		for(String pat : shiftDatePatterns)
		{
			try 
			{
				dateFormat.applyPattern(pat);
				fromDate = dateFormat.parse(ds); 
				return true;
			}
			catch(ParseException ex) { }
		}
		showError("Invalid 'from' time, Enter YYYY/MM/DD HH:MM.");
		return false;
	}

	public boolean toFieldEntered()
	{
		String ds = toField.getText().trim();
		toDate = null;
		if (ds.length() == 0)
			return true;
		for(String pat : shiftDatePatterns)
		{
			try 
			{
				dateFormat.applyPattern(pat);
				toDate = dateFormat.parse(ds); 
				return true;
			}
			catch(ParseException ex) { }
		}
		showError("Invalid 'to' time, Enter YYYY/MM/DD HH:MM.");
		return false;
	}

	public String getTitle()
	{
		return "Decode and Apply Time Shift";
	}

	public void activate()
	{
	}

	public boolean deactivate()
	{
		return true;
	}

	public void startDecodePressed()
	{
		FileIdPanel fileIdPanel = getFileIdPanel();
		if (fileIdPanel.rawMessage == null)
		{
			showError("No data has been scanned yet! "
				+ "Go back to previous panel, open a file, and press the 'Scan'"
				+ " button.");
			return;
		}

		Platform platform = fileIdPanel.getSelectedPlatform();
		DecodesScript decodesScript = fileIdPanel.getSelectedScript();

		if (platform == null
		 || decodesScript == null)
		{
			showError("The 'scan' was not able to associate this data "
				+ " with a platform, site, or transport medium."
				+ "Go back to previous panel and supply the missing "
				+ "information.");
			return;
		}
		if (!getShiftInfo())
			return;

		if (traceDialog == null)
		{
			traceDialog = new TraceDialog(TopFrame.instance(), false);
			traceLogButton.setEnabled(true);
		}
		traceDialog.clear();
		Logger origLogger = Logger.instance();
		TraceLogger traceLogger = new TraceLogger(origLogger.getProcName());
		traceLogger.setDialog(traceDialog);
		TeeLogger teeLogger = new TeeLogger(origLogger.getProcName(), 
			origLogger, traceLogger);
		traceLogger.setMinLogPriority(origLogger.getMinLogPriority());
		Logger.setLogger(teeLogger);

//System.out.println("Decoding with platform '" + platform.makeFileName() + "'");
		Site origSite = platform.getSite();
		Site newSite = fileIdPanel.getSelectedSite();
		if (newSite != null && newSite != origSite)
		{
//System.out.println("Setting site to '" + newSite.getDisplayName() + "'");
			platform.setSite(newSite);
		}
		TransportMedium tm = null;
		try
		{
			platform.prepareForExec();
			if (decodesScript == null)
			{
				showError("The transport medium selected has an invalid or "
					+ "missing DECODES script. Go back to previous panel and "
					+ "make sure the setting for Medium Type is correct.");
				return;
			}
			try 
			{
				tm = fileIdPanel.rawMessage.getTransportMedium();
			}
			catch(UnknownPlatformException ex)
			{
//System.out.println("No TM associated in message, searching for matching script.");
				for(Iterator it = platform.transportMedia.iterator();
					it.hasNext(); )
				{
					tm = (TransportMedium)it.next();
					if (tm.scriptName.equalsIgnoreCase(
						decodesScript.scriptName))
					{
						fileIdPanel.rawMessage.setTransportMedium(tm);
//System.out.println("Set TM to '" + tm.getTmKey() + "'");
						break;
					}
				}
			}
			fileIdPanel.rawMessage.setPlatform(platform);
			decodedMessage = 
				decodesScript.decodeMessage(fileIdPanel.rawMessage);
			decodedMessage.applyScaleAndOffset();
			decodedMessage.applySensorLimits();
			presentationGroup = fileIdPanel.getPresentationGroup();
			presentationGroup.prepareForExec();
			decodedMessage.formatSamples(presentationGroup);
			applyTimeShifts(decodedMessage);

			StringBufferConsumer consumer =
				new StringBufferConsumer(new StringBuffer());
//			String tzs = DecodesSettings.instance().decwizTimeZone;
			String tzs = tm.getTimeZone();
			if (tzs == null || tzs.length() == 0)
				tzs = "UTC";
			else if ( tzs.matches("^GMT.*[MAYN]$") ) 
				tzs = tzs.substring(0,tzs.length()-1);
			tzs = tzs.trim();
			OutputFormatter formatter = OutputFormatter.makeOutputFormatter(
				fileIdPanel.getSelectedFormat(), TimeZone.getTimeZone(tzs), 
				null, new Properties());
			formatter.formatMessage(decodedMessage, consumer); 
			decodedDataArea.setText(consumer.getBuffer().toString());
			try 
			{
				maxMissing = Integer.parseInt(maxMissingField.getText().trim());
			}
			catch(NumberFormatException ex)
			{
				showError("Max Missing must be an integer, defaulting to 4.");
				maxMissing = 4;
				maxMissingField.setText("4");
			}
			sumRepGen.setMaxGapSize(maxMissing);
			sumRepGen.setTimeZone(tzs);
			summaryArea.setText(
				sumRepGen.makeReport(decodedMessage,fileIdPanel.getFileName()));
		}
		catch(DecoderException ex)
		{
			showError("Error decoding: " + ex);
		}
		catch(DatabaseException ex)
		{
			showError("Error in DECODES Database: " + ex);
		}
		catch(OutputFormatterException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
			showError("Error in Output Formatter: " + ex);
		}
		catch(DataConsumerException ex)
		{
			showError("Error in String Buffer Consumer: " + ex);
		}
		catch(Exception ex)
		{
			String msg = "Unexpected exception: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			showError("Unexpected exception: " + ex);
		}
		finally
		{
			Logger.setLogger(origLogger);
			if (newSite != null && newSite != origSite && platform != null)
				platform.setSite(origSite);
		}
	}

	private void traceLogButtonPressed()
	{
		traceDialog.setVisible(true);
	}

	private void timeShiftCheckPressed()
	{
		boolean on = timeShiftCheck.isSelected();
		fromLabel.setEnabled(on);
		toLabel.setEnabled(on);
		shiftLabel.setEnabled(on);
		fromField.setEnabled(on);
		toField.setEnabled(on);
		shiftField.setEnabled(on);
	}

	private void applyTimeShifts(DecodedMessage decodedMessage)
	{
		if (!timeShiftCheck.isSelected() || shiftValue == 0L)
			return;

		for(Iterator tsit = decodedMessage.getAllTimeSeries(); tsit.hasNext();)
		{
			TimeSeries ts = (TimeSeries)tsit.next();
			int nsamps = ts.size();
			for(int i = 0; i<nsamps; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				long sampt = tv.getTime().getTime();
				if ((fromDate == null || sampt >= fromDate.getTime())
				 && (toDate   == null || sampt <= toDate.getTime()))
				{
					sampt += shiftValue;
					tv.setTime(new Date(sampt));
				}
			}
		}
	}
	public Date getLastTimeOfDecodedData() {
		Date lastDate = null;
		if ( decodedMessage != null )
		{ 
			for(Iterator tsit = decodedMessage.getAllTimeSeries(); tsit.hasNext();)
			{
				TimeSeries ts = (TimeSeries)tsit.next();
				Date dt = ts.timeOfLastSampleInSeries();
				if ( dt != null ) {
					if ( lastDate == null )
						lastDate = dt;
					else if ( lastDate.before(dt) )
						lastDate = dt;
				}
			}
		}
		return lastDate;
	}
	public String getDecodedData()
	{
		return decodedDataArea.getText();
	}

	public String getSummaryData()
	{
		return summaryArea.getText();
	}

	public void clearData()
	{
		decodedDataArea.setText("");
		summaryArea.setText("");
	}
}
