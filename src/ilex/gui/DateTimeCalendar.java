package ilex.gui;

import javax.swing.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The DateTimeCalendar is a wrapper around the DateCalendar and the
 * TimeSpinner. It creates a JPanel with a JCalendar widget and
 * a time spinner widget.
 * 
 */
@SuppressWarnings("serial")
public class DateTimeCalendar extends JPanel
{
	public static final long MS_PER_DAY = 24 * 3600 * 1000L;
	public static final String defaultTZ = "EST5EDT";

	private DateCalendar dateCalendar = null;
	private TimeSpinner timeSpinner;
	private TimeZone tzObj;
	private FlowLayout panelLayout = new FlowLayout(
		FlowLayout.CENTER, 4, 0);
	
	/**
	 * Construct a Calendar - Time Panel. It allows you to select a 
	 * date from a calendar widget and the time from a time spinner.
	 * In addition, it allows to set a timezone.
	 * 
	 * @param text String - label to identified this DateCalendar. Ex. From
	 * 				It should be provided. Default is no label
	 * @param dateIn Date - default date for DateCalendar and time Spinner 
	 * 						If null, default to today's date at 00:00:00
	 * @param dateFormatString String - format for JCalendar Date text field 
	 * 									Deafult format: MMM d,yyyy
	 * @param timeZoneStr String - the timezone java id, Default is defaultTZ above
	 */
	public DateTimeCalendar(String text, 
							Date dateIn, 
							String dateFormatString,
							String timeZoneStr)
	{
//System.out.println("dtc('" + text + "', " + dateIn + ", " + dateFormatString + ", " + timeZoneStr + ")");
		String dateLabel = text;
		if (dateLabel == null)
			dateLabel = "";
		
		// Default TZ to UTC if not supplied.
		if (timeZoneStr == null || timeZoneStr.equals(""))
			tzObj = TimeZone.getTimeZone(defaultTZ);
		else
			tzObj = TimeZone.getTimeZone(timeZoneStr);	

		
		// Default to noon in current day in whatever tz was specified
		if (dateIn == null)
		{	
			dateIn = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(tzObj);
			cal.setTime(dateIn);
			cal.set(Calendar.HOUR_OF_DAY, 12);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			dateIn = cal.getTime();
		}
		
		dateCalendar = 
			new DateCalendar(text, dateIn, dateFormatString, tzObj);
		timeSpinner = new TimeSpinner(dateIn);
		//Need to set timezone, pass it as a parameter
		timeSpinner.setTimeZone(tzObj);
		
		try
		{
			jbInit();
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void jbInit() throws Exception
	{
		this.setLayout(panelLayout);
		this.add(dateCalendar);
		this.add(timeSpinner);
	}

	public JPanel getDateTimePanel()
	{
		return this;
	}

	public Date getDate()
	{
		// Get date from DateCalendar and date from TimeSpinner
		Date dateFCalendar = dateCalendar.getDate();
		Date dateTimeSpinner = timeSpinner.getTime();

//System.out.println("dtc.getDate fromCal=" + dateFCalendar + ", fromTime=" + dateTimeSpinner);
		if (dateFCalendar == null || dateTimeSpinner == null)
			return null;

		// Need to construct a new Date Obj with the date from
		// DateCalendar and time from TimeSpinner, and return the
		// new date object

		//tempCal1 holds the Date obj from the DateCalendar class
		Calendar tempCal1 = new GregorianCalendar();
		tempCal1.setTimeZone(tzObj);
		tempCal1.setTime(dateFCalendar);
//System.out.println("dtc.getDate tempCal1=" + tempCal1.getTime());
		
		//Get the Date obj from Time Spinner and extract out the 
		//time values (hour, minutest, seconds)
		Calendar tempCal2 = new GregorianCalendar();
		//tempCal2.setTimeZone(TimeZone.getDefault());
		tempCal2.setTimeZone(tzObj);
		tempCal2.setTime(dateTimeSpinner);
//System.out.println("dtc.getDate tempCal2=" + tempCal2.getTime());
				
		//Get the components of the time
		int hour24 = tempCal2.get(Calendar.HOUR_OF_DAY); // 0..23
		int min = tempCal2.get(Calendar.MINUTE); // 0..59
		int sec = tempCal2.get(Calendar.SECOND); // 0..59

		//Set the hour, min, sec of the Date obj that we are going to 
		//return
		tempCal1.set(Calendar.HOUR_OF_DAY, hour24);
		tempCal1.set(Calendar.MINUTE, min);
		tempCal1.set(Calendar.SECOND, sec);
//System.out.println("after setting time values: tempCal1=" + tempCal1.getTime());
		
		tempCal1.setTimeZone(tzObj);
		
//System.out.println("after setting TZ, tempCal1=" + tempCal1.getTime());

		Date returnDate = new Date(tempCal1.getTimeInMillis());
//System.out.println("Returning " + returnDate);
		
		// Return Date obj
		return returnDate;
	}

	public void setDate(Date dateIn)
	{
		//Set DateCalendar with the date and TimeSpinner 
		//with the time, DateCalendar has a setDate()
		//TimeSpinner has a setTime()
		dateCalendar.setDate(dateIn);
		timeSpinner.setTime(dateIn);
	}

	public void setEnabled(boolean tf)
	{
		dateCalendar.setEnabled(tf);
		timeSpinner.setEnabled(tf);
	}
	
	public void stopEditing()
	{
		timeSpinner.stopEditing();
	}
	
	public static void main(String[] args)
	{
		final DateTimeCalendar datetimecalendar = new DateTimeCalendar("Test" , new Date() , null, null);
		Frame testFrame = new Frame();

		testFrame.setVisible(true);
		testFrame.add(datetimecalendar.getDateTimePanel());
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit()
				.getScreenSize();
		testFrame.setSize(new java.awt.Dimension(350, 100));
		testFrame.setLocation((screenSize.width - 826) / 2,
				(screenSize.height - 709) / 2);
		testFrame.setVisible(true);
		testFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Date merge = " + datetimecalendar.getDate());
				System.out.println("");
			}
		});
			
	}
}
