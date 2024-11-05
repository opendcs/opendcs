package ilex.gui;

import javax.swing.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;

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
	public static final String defaultTZ = "UTC";

	private DateCalendar dateCalendar = null;
	private TimeSpinner timeSpinner;
	private TimeZone tzObj;
	private FlowLayout panelLayout = new FlowLayout(
		FlowLayout.CENTER, 4, 0);
	private SimpleDateFormat sdf;
	
	/**
	 * Construct a Calendar - Time Panel. It allows you to select a 
	 * date from a calendar widget and the time from a time spinner.
	 * In addition, it allows to set a timezone.
	 * 
	 * @param text String - label to identified this DateCalendar. Ex. From
	 * 				It should be provided. Default is no label
	 * @param dateIn Date - default date for DateCalendar and time Spinner 
	 * 						If null, default to today's date at 00:00:00
	 * @param dateFmt String - format for JCalendar Date text field 
	 * 									Deafult format: MMM d,yyyy
	 * @param tzid String - the timezone java id, Default is defaultTZ above
	 */
	public DateTimeCalendar(String text, Date dateIn, String dateFmt, String tzid)
	{
		String dateLabel = text;
		if (dateLabel == null)
			dateLabel = "";
		
		// Default TZ to UTC if not supplied.
		if (tzid == null || tzid.equals(""))
			tzObj = TimeZone.getTimeZone(defaultTZ);
		else
			tzObj = TimeZone.getTimeZone(tzid);	

		sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(tzObj);
		
//System.out.println("DateTimeCalendar('" + text + "', " + sdf.format(dateIn) + ", " 
//+ dateFmt + ", " + tzid + ")");

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
		
		dateCalendar = new DateCalendar(text, dateIn, dateFmt, tzObj);
		timeSpinner = new TimeSpinner(dateIn);
		timeSpinner.setTimeZone(tzObj);
		
		try
		{
			jbInit();
		} 
		catch (Exception ex)
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

//System.out.println("dtc.getDate fromCal=" + sdf.format(dateFCalendar));
		if (dateFCalendar == null)
			return null;

		Calendar tempCal1 = Calendar.getInstance();
		tempCal1.setTimeZone(tzObj);
		tempCal1.setTime(dateFCalendar);
//System.out.println("dtc.getDate tempCal1=" + sdf.format(tempCal1.getTime()));
		timeSpinner.updateCalendar(tempCal1);
		
		Date ret = tempCal1.getTime();
//System.out.println("after setting time values: tempCal1=" + sdf.format(ret));
		
		// Return Date obj
		return ret;
	}

	public void setDate(Date dateIn)
	{
//System.out.println("dtc.setDate(" + sdf.format(dateIn));
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
		final DateTimeCalendar datetimecalendar = new DateTimeCalendar("(UTC)" , new Date() , "dd/MMM/yyyy", "UTC");

		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, -1);
		datetimecalendar.setDate(cal.getTime());

		
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
