package ilex.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.MaskFormatter;

import com.toedter.calendar.DateUtil;
import com.toedter.calendar.IDateEditor;

/**
 * JTextFieldDateEditor is the default editor used by JDateChooser. 
 * This contains a set of the timezone.
 */
public class JTextFieldDateEditor extends JFormattedTextField 
	implements IDateEditor,	CaretListener, FocusListener, ActionListener 
{

	private static final long serialVersionUID = -8901842591101625304L;
	protected Date date;
	protected SimpleDateFormat dateFormatter;
	protected MaskFormatter maskFormatter;
	protected String datePattern;
	protected String maskPattern;
	protected char placeholder;
	protected Color darkGreen;
	protected DateUtil dateUtil;
	private boolean isMaskVisible;
	private boolean ignoreDatePatternChange;
	private Calendar calendar;
	private TimeZone timeZone;
	
	/** 
	 * Constructor that sets the timezone of the JCalendar
	 * @param TimeZone used to display the date (is required)
	 **/	
	public JTextFieldDateEditor(TimeZone tzObjIn) 
	{		
		this(false, "yyyy/MM/dd", null, ' ', tzObjIn);
	}

	public JTextFieldDateEditor(String datePattern, String maskPattern, 
								char placeholder,TimeZone tzObjIn) 
	{
		this(true, datePattern, maskPattern, placeholder, tzObjIn);
	}

	public JTextFieldDateEditor(boolean showMask, String datePattern, 
					String maskPattern, char placeholder, TimeZone tzObjIn) 
	{
		timeZone = tzObjIn == null ? TimeZone.getTimeZone("UTC") : tzObjIn;
		if (datePattern == null)
			datePattern = "yyyy/MM/dd";
		
		dateFormatter = new SimpleDateFormat(datePattern);
		dateFormatter.setLenient(false);
		dateFormatter.setTimeZone(timeZone);
		
		setDateFormatString(datePattern);
		if (datePattern != null) {
			ignoreDatePatternChange = true;
		}

		this.placeholder = placeholder;

		if (maskPattern == null) {
			this.maskPattern = createMaskFromDatePattern(this.datePattern);
		} else {
			this.maskPattern = maskPattern;
		}

		setToolTipText(this.datePattern);
		setMaskVisible(showMask);

		addCaretListener(this);
		addFocusListener(this);
		addActionListener(this);
		darkGreen = new Color(0, 150, 0);

		//Josue added
		calendar = Calendar.getInstance(timeZone);		
		dateFormatter.setCalendar(calendar);
		//End		
		dateUtil = new DateUtil();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#getDate()
	 */
	public Date getDate() 
	{
		try 
		{
			String t = getText();
			
			// This will set my calendar object associated with the formatter.
			dateFormatter.parse(getText());
			
			// Set to noon to handle most time zones.
			calendar.set(Calendar.HOUR_OF_DAY, 12);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			
			date = calendar.getTime();
//System.out.println("jtfde.getDate text='" + t + "' ret=" + date);
			return date;
		}
		catch (ParseException e)
		{
			date = null;
		}
		return date;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#setDate(java.util.Date)
	 */
	public void setDate(Date date) {
		setDate(date, true);
	}

	/**
	 * Sets the date.
	 * 
	 * @param date
	 *            the date
	 * @param firePropertyChange
	 *            true, if the date property should be fired.
	 */
	protected void setDate(Date date, boolean firePropertyChange) {
		Date oldDate = this.date;
		this.date = date;

		if (date == null) 
		{
			setText("");
		} else 
		{
//System.out.println("jtfde setDate(" + date + ")");
			calendar.setTime(date);
			calendar.set(Calendar.HOUR_OF_DAY, 12);
//System.out.println("JTextFieldDateEditor setDate - calendar after setTime=" + calendar.getTime());
//			hours = calendar.get(Calendar.HOUR_OF_DAY);
//			minutes = calendar.get(Calendar.MINUTE);
//			seconds = calendar.get(Calendar.SECOND);
//			millis = calendar.get(Calendar.MILLISECOND);
//			//Josue Added
//			calendar.setTimeZone(timeZone);
//System.out.println("JTextFieldDateEditor setDate - calendar after setting fields=" + calendar.getTime());

			String formattedDate = dateFormatter.format(date);

			try {
				setText(formattedDate);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		if (date != null && dateUtil.checkDate(date)) {
			setForeground(Color.BLACK);

		}

		if (firePropertyChange) {
			firePropertyChange("date", oldDate, date);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.
	 * IDateEditor#setDateFormatString(java.lang.String)
	 */
	public void setDateFormatString(String dateFormatString)
	{
//System.out.println("setDateFormatString(" + dateFormatString + ")");
		if (ignoreDatePatternChange) {
			return;
		}

		try {
			dateFormatter.applyPattern(dateFormatString);
		} catch (RuntimeException e) 
		{
			Calendar cal = Calendar.getInstance(timeZone);			
			dateFormatter = 
			  (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
			dateFormatter.setLenient(false);
			dateFormatter.setCalendar(cal);
		}
		this.datePattern = dateFormatter.toPattern();
		setToolTipText(this.datePattern + 
				" (" + timeZone.getDisplayName() +")");
				//" (Enter Date/Time in " + timeZone.getDisplayName() +")");
		setDate(date, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#getDateFormatString()
	 */
	public String getDateFormatString() {
		return datePattern;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#getUiComponent()
	 */
	public JComponent getUiComponent() {
		return this;
	}

	/**
	 * After any user input, the value of the textfield is proofed. 
	 * Depending on being a valid date, the value is colored green or red.
	 * 
	 * @param event
	 *            the caret event
	 */
	public void caretUpdate(CaretEvent event) {
		String text = getText().trim();
		String emptyMask = maskPattern.replace('#', placeholder);

		if (text.length() == 0 || text.equals(emptyMask)) {
			setForeground(Color.BLACK);
			return;
		}

		try {
			Date date = dateFormatter.parse(getText());
			if (dateUtil.checkDate(date)) {
				setForeground(darkGreen);
			} else {
				setForeground(Color.RED);
			}
		} catch (Exception e) {
			setForeground(Color.RED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	public void focusLost(FocusEvent focusEvent) {
		checkText();
	}

	private void checkText() {
		try {
			Date date = dateFormatter.parse(getText());
			setDate(date, true);
		} catch (Exception e) {
			// ignore
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.Component#setLocale(java.util.Locale)
	 */
	public void setLocale(Locale locale) {
		if (locale == getLocale() || ignoreDatePatternChange) {
			return;
		}

		super.setLocale(locale);
		dateFormatter = 
			(SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, 
															locale);
		setToolTipText(dateFormatter.toPattern());

		setDate(date, false);
		doLayout();
	}

	/**
	 * Creates a mask from a date pattern. This is a very simple (and
	 * incomplete) implementation thet works only with numbers. A date pattern
	 * of "MM/dd/yy" will result in the mask "##/##/##". Probably you want to
	 * override this method if it does not fit your needs.
	 * 
	 * @param datePattern
	 *            the date pattern
	 * @return the mask
	 */
	public String createMaskFromDatePattern(String datePattern) {
		String symbols = "GyMdkHmsSEDFwWahKzZ";
		String mask = "";
		for (int i = 0; i < datePattern.length(); i++) {
			char ch = datePattern.charAt(i);
			boolean symbolFound = false;
			for (int n = 0; n < symbols.length(); n++) {
				if (symbols.charAt(n) == ch) {
					mask += "#";
					symbolFound = true;
					break;
				}
			}
			if (!symbolFound) {
				mask += ch;
			}
		}
		return mask;
	}

	/**
	 * Returns true, if the mask is visible.
	 * 
	 * @return true, if the mask is visible
	 */
	public boolean isMaskVisible() {
		return isMaskVisible;
	}

	/**
	 * Sets the mask visible.
	 * 
	 * @param isMaskVisible
	 *            true, if the mask should be visible
	 */
	public void setMaskVisible(boolean isMaskVisible) {
		this.isMaskVisible = isMaskVisible;
		if (isMaskVisible) {
			if (maskFormatter == null) {
				try {
					maskFormatter = 
					 new MaskFormatter(createMaskFromDatePattern(datePattern));
					maskFormatter.setPlaceholderCharacter(this.placeholder);
					maskFormatter.install(this);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Returns the preferred size. If a date pattern is set, 
	 * it is the size the date pattern would take.
	 */
	public Dimension getPreferredSize() {
		if (datePattern != null) {
			return new JTextField(datePattern).getPreferredSize();
		}
		return super.getPreferredSize();
	}

	/**
	 * Validates the typed date and sets it (only if it is valid).
	 */
	public void actionPerformed(ActionEvent e) {
		checkText();
	}

	/**
	 * Enables and disabled the compoment. It also fixes the background bug
	 * 4991597 and sets the background explicitely to a
	 * TextField.inactiveBackground.
	 */
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		if (!b) {
			super.setBackground(UIManager.getColor("TextField.inactiveBackground"));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#getMaxSelectableDate()
	 */
	public Date getMaxSelectableDate() {
		return dateUtil.getMaxSelectableDate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#getMinSelectableDate()
	 */
	public Date getMinSelectableDate() {
		return dateUtil.getMinSelectableDate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#setMaxSelectableDate(java.util.Date)
	 */
	public void setMaxSelectableDate(Date max) {
		dateUtil.setMaxSelectableDate(max);
		checkText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#setMinSelectableDate(java.util.Date)
	 */
	public void setMinSelectableDate(Date min) {
		dateUtil.setMinSelectableDate(min);
		checkText();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.toedter.calendar.IDateEditor#setSelectableDateRange(java.util.Date,
	 *      java.util.Date)
	 */
	public void setSelectableDateRange(Date min, Date max) {
		dateUtil.setSelectableDateRange(min, max);
		checkText();
	}

	/**
	 * Creates a JFrame with a JCalendar inside and can be used for testing.
	 * 
	 * @param s
	 *            The command line arguments
	 */
	public static void main(String[] s) {
		JFrame frame = new JFrame("JTextFieldDateEditor");
		JTextFieldDateEditor jTextFieldDateEditor = 
			new JTextFieldDateEditor(TimeZone.getTimeZone("UTC"));
		jTextFieldDateEditor.setDate(new Date());
		frame.getContentPane().add(jTextFieldDateEditor);
		frame.pack();
		frame.setVisible(true);
	}
}
