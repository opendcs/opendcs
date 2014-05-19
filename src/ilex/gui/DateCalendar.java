package ilex.gui;

import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import javax.swing.JComponent;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.Dimension;

import com.toedter.calendar.IDateEditor;
import com.toedter.calendar.JDateChooser;

import java.util.Date;
import java.awt.*;
import javax.swing.*;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
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
public class DateCalendar extends JPanel
{
	private GridBagLayout dateGridbag = new GridBagLayout();
	private String dateLabel = "";
	private JComponent dateComponent;
	private GridBagLayout gridBagLayoutMain = new GridBagLayout();
	public JLabel textLabel = new JLabel();
	public JTextFieldDateEditor textFieldEditor ;
	//private JPanel datePanel = new JPanel();
	//private BorderLayout datePanelBorderLayout = new BorderLayout();

	/**
	 * Constructor for DateCalendar.
	 * 
	 * @param dateLabelIn
	 *            String label to identified this DateCalendar. Ex. From
	 * @param dateIn
	 *            Date default for DateCalendar
	 * @param dateFormatString
	 *            String format for date
	 * @param timezone object
	 */
	public DateCalendar(String dateLabelIn, Date dateIn, 
						String dateFormatString, 
						TimeZone tzObjIn)
	{		 
		dateLabel = dateLabelIn;
		if (dateLabel == null)
			dateLabel = "";
		Date dateToSet = dateIn;
		if (dateToSet == null)
			dateToSet = new Date();
		TimeZone tzObj = tzObjIn;
		if (tzObj == null)
			tzObj = TimeZone.getTimeZone("UTC");
	
		textFieldEditor = new JTextFieldDateEditor(tzObj);
		
		dateComponent = new JDateChooser(dateToSet, dateFormatString, 
										textFieldEditor);
		//((JDateChooser) dateComponent).setTimeZone(tzObj);//no need for this
		
		try
		{
			jbInit();
		} catch (Exception ex)
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

//		datePanel.setLayout(datePanelBorderLayout);
//		datePanel.setPreferredSize(new Dimension(30, 20));
//		datePanel.add(dateComponent, BorderLayout.CENTER);

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
			return ((JDateChooser) dateComponent).getDate();
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
		if (dateComponent != null)
			((JDateChooser) dateComponent).setDate(dateIn);
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
				null, TimeZone.getTimeZone("UTC"));

		Date testDate = dateCalendarFrom.getDate();
		System.out.println("date from JCalendar= " + testDate);

		testFrame.add(dateCalendarFrom.getDateCalendarPanel());
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		testFrame.setSize(new java.awt.Dimension(200, 100));
		testFrame.setLocation((screenSize.width - 826) / 2,
				(screenSize.height - 709) / 2);
		// ========= TEST Calendar
		Calendar cal2 = new GregorianCalendar();
		cal2.setTime(testDate);
		// Get the components of the time
		int hour12 = cal2.get(Calendar.HOUR); // 0..11
		int hour24 = cal2.get(Calendar.HOUR_OF_DAY); // 0..23
		int min = cal2.get(Calendar.MINUTE); // 0..59
		int sec = cal2.get(Calendar.SECOND); // 0..59

		System.out.println("hour12 local= " + hour12);
		System.out.println("hour24 local = " + hour24);
		System.out.println("min local = " + min);
		System.out.println("sec local = " + sec);

		// Get the current hour-of-day at GMT

		cal2.setTimeZone(TimeZone.getTimeZone("UTC"));
		int hour24b = cal2.get(Calendar.HOUR_OF_DAY); // 0..23
		System.out.println("hour24 in UTC= " + hour24b);

		// Get time in milliseconds
		System.out.println("time in milli = " + cal2.getTimeInMillis());
		Date utcDate = new Date(cal2.getTimeInMillis());
		System.out.println("date obj set with cal2.getTimeInMillis =  "
				+ utcDate);

		testFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				Date testDate1 = dateCalendarFrom.getDate();
				System.out.println("date from JCalendar= " + testDate1);
				Calendar cal3 = new GregorianCalendar();
				cal3.setTime(testDate1);
				// Get the components of the time
				int hour12 = cal3.get(Calendar.HOUR); // 0..11
				int hour24 = cal3.get(Calendar.HOUR_OF_DAY); // 0..23
				int min = cal3.get(Calendar.MINUTE); // 0..59
				int sec = cal3.get(Calendar.SECOND); // 0..59

				System.out.println("hour12 local= " + hour12);
				System.out.println("hour24 local = " + hour24);
				System.out.println("min local = " + min);
				System.out.println("sec local = " + sec);

				// Get the current hour-of-day at GMT

				cal3.setTimeZone(TimeZone.getTimeZone("UTC"));
				int hour24b = cal3.get(Calendar.HOUR_OF_DAY); // 0..23
				System.out.println("hour24 in UTC= " + hour24b);

				// Get time in milliseconds
				System.out.println("time in milli =" + cal3.getTimeInMillis());
				Date utcDate = new Date(cal3.getTimeInMillis());
				System.out.println("date obj set with cal3.getTimeInMillis =  "
						+ utcDate);

				DateFormat dateFormat = 
					new SimpleDateFormat("yyyyMMdd HHmmss");
				dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				// try
				// {
				// Date retDate = dateFormat.parse(utcDate);
				// }
				// catch (ParseException ex) {}

			}
		});
		testFrame.setVisible(true);
	}
}
