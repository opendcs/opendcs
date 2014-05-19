package ilex.gui;

import javax.swing.*;
import java.util.*;
import javax.swing.event.*;
public class TimeSpinner extends JPanel{
	//private JTextField mytext=new JTextField();
	public JSpinner myspinner = new JSpinner();
	private SpinnerDateModel mymodel;
	private Date mydate = new Date();
	TimeSpinner(Date mydate)
	{
		this.mydate = mydate;
		mymodel=new SpinnerDateModel();
		myspinner.setModel(mymodel);
		myspinner.setValue(mydate);
		myspinner.setEditor(new JSpinner.DateEditor(myspinner, "HH:mm:ss"));
		
		add(myspinner);
		
	}
	public Date getTime()
	{
		return (Date)myspinner.getValue();
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
}
