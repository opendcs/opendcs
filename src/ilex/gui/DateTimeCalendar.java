package ilex.gui;

import javax.swing.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
	private DateCalendar dateCalendar = null;
	private TimeSpinner timeSpinner;
	private Date dateToSet = null;
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
	 * @param timeZoneStr String - the timezone java id, Default is UTC
	 */
	public DateTimeCalendar(String text, 
							Date dateIn, 
							String dateFormatString,
							String timeZoneStr)
	{
		String dateLabel = text;
		if (dateLabel == null)
			dateLabel = "";
		dateToSet = dateIn;
		if (dateToSet == null)
		{
			dateToSet = new Date();//default time to 00:00:00
			long MS_PER_DAY1 = 86400000; //1 day = 86400000 milliseconds
			long timeInMs1 = dateToSet.getTime();
			timeInMs1 = (timeInMs1/MS_PER_DAY1) * MS_PER_DAY1;
			// Set hour to 00:00:00 				
			dateToSet.setTime(timeInMs1);
		}
		if (timeZoneStr == null || timeZoneStr.equals(""))
		{
			tzObj = TimeZone.getTimeZone("UTC");
		}
		else
		{
			tzObj = TimeZone.getTimeZone(timeZoneStr);	
		}
		dateCalendar = 
			new DateCalendar(text, dateToSet, dateFormatString, tzObj);
		timeSpinner = new TimeSpinner(dateToSet);
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
//		timeSpinner.setPreferredSize(new Dimension(85, 30));
		this.setLayout(panelLayout);
//		dateCalendar.setPreferredSize(new Dimension(175, 20));
//		this.setMinimumSize(new Dimension(275, 45));
//		this.setPreferredSize(new Dimension(275, 45));
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
		//args: label, date string, timezone
		
		
		Date startTime1 = null;
		TimeZone tz = TimeZone.getTimeZone("UTC");
		SimpleDateFormat dateFmt;
		dateFmt = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
		dateFmt.setTimeZone(tz);		
		try
		{
			startTime1 = dateFmt.parse("2007-01-01 09:01:10");
		} catch (ParseException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Date todaysDate = new Date(); //Default to today's Date
		long MS_PER_DAY1 = 86400000; //1 day = 86400000 milliseconds
		long timeInMs1 = todaysDate.getTime();
		timeInMs1 = (timeInMs1/MS_PER_DAY1) * MS_PER_DAY1;
		// Set hour to 00:00:00 				
		todaysDate.setTime(timeInMs1);
		SimpleDateFormat df1 = 
			new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
		
		df1.setTimeZone(TimeZone.getTimeZone("UTC"));
		//df1.setTimeZone(TimeZone.getTimeZone("EST5EDT"));
		System.out.println("Date to be sent to JCalendar= " +
				df1.format(todaysDate));
				
		final DateTimeCalendar datetimecalendar =           //"EST5EDT"
			new DateTimeCalendar("From" ,null , null, null);
		datetimecalendar.setDate(todaysDate);
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
				//System.out.println("Date merge = " + 
				//		datetimecalendar.getDate());				
				SimpleDateFormat df = 
					new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
				
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				System.out.println("Date merge = " +
						df.format(datetimecalendar.getDate()));
				
				//another dummy test reset the calendar
				Date retDate = new Date(); //Default to today's Date
	            long MS_PER_DAY = 86400000; //1 day = 86400000 milliseconds
	            long timeInMs = retDate.getTime();//set time to 23:59:59
	            timeInMs = (timeInMs/MS_PER_DAY) * MS_PER_DAY;
                timeInMs = timeInMs + MS_PER_DAY -1;
                retDate.setTime(timeInMs);
				datetimecalendar.setDate(retDate);
			}
		});
			
	}
}
