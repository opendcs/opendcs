package ilex.gui;

import javax.swing.*;

import java.awt.Component;
import java.util.*;
import javax.swing.event.*;
public class TimeSpinner 
	extends JPanel
{
	public JSpinner myspinner = new JSpinner();
	private SpinnerDateModel mymodel;
	private Date mydate = new Date();
	private JSpinner.DateEditor editor;
	
	TimeSpinner(Date mydate)
	{
		this.mydate = mydate;
		mymodel=new SpinnerDateModel();
		myspinner.setModel(mymodel);
		myspinner.setValue(mydate);
		editor = new JSpinner.DateEditor(myspinner, "HH:mm:ss");
		myspinner.setEditor(editor );
		
		add(myspinner);
		
	}
	public Date getTime()
	{
		Date ret = (Date)myspinner.getValue();
		return ret;
	}
	public void setTime(Date newtime)
	{
		myspinner.setValue(newtime);
	}
	
	public void setEnabled(boolean tf)
	{
		myspinner.setEnabled(tf);
	}
	
	/**
	 * sets timezone for display on the spinner
	 * @param newzone new timezone for display as a TimeZone object
	 */
	public void setTimeZone(TimeZone newzone)
	{
		((JSpinner.DateEditor)myspinner.getEditor()).getFormat().setTimeZone(newzone);
		ChangeEvent e = new ChangeEvent(myspinner);
		((JSpinner.DateEditor)myspinner.getEditor()).stateChanged(e);
	}
	
	public void stopEditing()
	{
		if (myspinner.isFocusOwner())
			myspinner.transferFocus();
	}
	
	public void updateCalendar(Calendar cal)
	{
		String t = editor.getTextField().getText();
		String hms[] = t.split(":");
		try
		{
			if (hms.length >= 1)
				cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hms[0].trim()));
			if (hms.length >= 2)
				cal.set(Calendar.MINUTE, Integer.parseInt(hms[1].trim()));
			if (hms.length >= 3)
				cal.set(Calendar.SECOND, Integer.parseInt(hms[2].trim()));
		}
		catch(Exception ex) {}
	}
}
