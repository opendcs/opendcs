package ilex.gui;

import javax.swing.JPanel;

import java.awt.GridBagConstraints;

import javax.swing.JComponent;

import java.awt.GridBagLayout;

import javax.swing.JLabel;

import com.toedter.calendar.JDateChooser;

import java.util.Date;
import java.awt.*;

import javax.swing.*;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.text.DateFormat;

/**
 * The DateCalendar class is a wrapper arround JDateChooser class of the
 * Calendar package found on jcalendar-1.2.3.jar file. This class creates a
 * JPanel with a label text and a JDateChooser Component. The JDateChooser
 * component contains a textfield for displaying the date and a calendar icon.
 * When the user clicks on the icon a calendar window will come up. The user
 * will select a date from the calendar window.
 */
@SuppressWarnings("serial")
public class DateCalendar extends JPanel
{
	private String dateLabel = "";
	private JDateChooser dateComponent;
	private GridBagLayout gridBagLayoutMain = new GridBagLayout();
	public JLabel textLabel = new JLabel();
	public JTextFieldDateEditor textFieldEditor ;
	//private JPanel datePanel = new JPanel();
	//private BorderLayout datePanelBorderLayout = new BorderLayout();
	private SimpleDateFormat sdf;

	/**
	 * Constructor for DateCalendar.
	 * 
	 * @param label
	 *            String label to identified this DateCalendar. Ex. From
	 * @param dateIn
	 *            Date default for DateCalendar
	 * @param dateFmt
	 *            String format for date
	 * @param timezone object
	 */
	public DateCalendar(String label, Date dateIn, String dateFmt, TimeZone tzObj)
	{		 
		dateLabel = label;
		if (dateLabel == null)
			dateLabel = "";
		if (dateIn == null)
			dateIn = new Date();
		if (tzObj == null)
			tzObj = TimeZone.getTimeZone("UTC");
	
		sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(tzObj);

		textFieldEditor = new JTextFieldDateEditor(false, dateFmt, null, ' ', tzObj);
		
//System.out.println("DateCalendar ctor creating JDateChooser with date='" + sdf.format(dateIn) + "'"); 
		dateComponent = new JDateChooser(dateIn, dateFmt, textFieldEditor);
		
		try
		{
			jbInit();
		} 
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	// Add components to JPanel
	private void jbInit() throws Exception
	{
		this.setLayout(gridBagLayoutMain);
		textLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		textLabel.setText(dateLabel + ": ");

		this.add(textLabel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, 
				new Insets(0, 2, 0, 0), 5, 0));

		this.add(dateComponent, new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(0, 1, 0, 4), 10, 0));

	}

	/**
	 * This method returns the DateCalendar display object.
	 * 
	 * @return JPanel the DateCalendar panel
	 */
	public JPanel getDateCalendarPanel()
	{
		return this;
	}

	/**
	 * This method returns the date set by DateCalendar
	 * 
	 * @return Date date set by DateCalendar
	 */
	public Date getDate()
	{
		if (dateComponent != null)
		{
			Date r = dateComponent.getDate();
//System.out.println("DateCalendar.getDate returning " + sdf.format(r));
			return r;
		}
		else
			return null;
	}

	/**
	 * This method sets the DateCalendar date
	 * 
	 * @param dateIn
	 *            Date used to set DateCalendar date
	 */
	public void setDate(Date dateIn)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 12);
//System.out.println("DateCalendar.setDate to " + sdf.format(dateIn));
		if (dateComponent != null)
			dateComponent.setDate(dateIn);
	}

	/**
	 * Sets the locale. (for internationalization)
	 * 
	 * @param locale
	 *            Locale
	 */
	public void setLocale(Locale locale)
	{
		((JDateChooser) dateComponent).setLocale(locale);
	}

	public void setEnabled(boolean tf)
	{
		dateComponent.setEnabled(tf);
	}
	
	/**
	 * Main used ONLY for testing Purporses.
	 * 
	 * @param args String[]
	 */
	public static void main(String[] args)
	{
		Frame testFrame = new Frame();

		testFrame.setVisible(true);
		final DateCalendar dateCalendarFrom = new DateCalendar("From", null,
				"dd/MMM/yyyy", TimeZone.getTimeZone("UTC"));

		Date now = new Date();
		System.out.println("Setting to " + now);
		dateCalendarFrom.setDate(now);

		testFrame.add(dateCalendarFrom.getDateCalendarPanel());
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		testFrame.setSize(new java.awt.Dimension(200, 100));
		testFrame.setLocation((screenSize.width - 826) / 2,
				(screenSize.height - 709) / 2);

		testFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				Date testDate1 = dateCalendarFrom.getDate();
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				System.out.println("date from JCalendar= " + dateFormat.format(testDate1));
			}
		});
		testFrame.setVisible(true);
	}
}
