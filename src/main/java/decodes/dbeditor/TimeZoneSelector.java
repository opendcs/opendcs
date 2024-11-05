package decodes.dbeditor;

import javax.swing.JComboBox;
import java.util.TimeZone;
import java.util.Arrays;

import ilex.util.TextUtil;

/**
Combo Box for selecting from the (many) Java Time Zones.
*/
@SuppressWarnings("serial")
public class TimeZoneSelector extends JComboBox
{
	/** List of known java time zone IDs. */
	static String ids[] = TimeZone.getAvailableIDs();

	/** Constructor. */
    public TimeZoneSelector()
	{
        try
		{
            jbInit();

			addItem(""); // no selection is valid.
			Arrays.sort(ids);
			for(int i=0; i<ids.length; i++)
				addItem(ids[i]);

			// Default is no TZ selected (blank)
			setSelectedIndex(0);
			setEditable(true);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/** GUI component initialization. */
    private void jbInit() throws Exception {
    }

	/**
	  Sets the current selection.
	  @param tz the selection.
	*/
	public void setTZ(String tz)
	{
		if (tz == null || TextUtil.isAllWhitespace(tz))
			setSelectedIndex(0);
		else
		{
			if (tz.equalsIgnoreCase("Z"))
				tz = "UTC";

			for(int i=1; i<ids.length; i++)
				if (tz.equalsIgnoreCase(ids[i]))
				{
					setSelectedIndex(i+1);
					return;
				}
			addItem(tz);
			setSelectedItem(tz);
		}
	}

	/**
	 * @return currently selected timezone as a string.
	 */
	public String getTZ()
	{
		if (getSelectedIndex() == 0)
			return null;
		return (String)getSelectedItem();
	}
}
