package decodes.gui;

import decodes.sql.PlatformListIO;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.VarFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * TimeSeriesLine is used to hold and transfer CTimeSeries data
 * into charting tools as a graphical line
 */
public class TimeSeriesLine
{
    private final static Logger log = LoggerFactory.getLogger(TimeSeriesLine.class);
    private final CTimeSeries cTimeSeries;
    private final Color color;
    private final ArrayList<Color> friendlyColors = new ArrayList<>();
    private ArrayList<Double> valueList;
    private ArrayList<Date> timeList;
    private static int colorIndex =0;

    public TimeSeriesLine (CTimeSeries cTimeSeries, Color color)
    {
     this.cTimeSeries = cTimeSeries;
     this.color = color;
     init();
    }
    public TimeSeriesLine (CTimeSeries cTimeSeries)
    {
        this.cTimeSeries = cTimeSeries;
        init();
        if( colorIndex >= friendlyColors.size())
        {
            colorIndex =0; // reset
        }

        this.color = friendlyColors.get(colorIndex++);

    }

    private void createColors()
    {
        friendlyColors.clear();
        friendlyColors.add(new Color(255, 166, 158));   // Peach
        friendlyColors.add(new Color(255, 205, 165));   // Apricot
        friendlyColors.add(new Color(240, 249, 232));   // Mint
        friendlyColors.add(new Color(204, 204, 255));   // Lavender
        friendlyColors.add(new Color(255, 153, 153));   // Salmon
        friendlyColors.add(new Color(209, 226, 243));   // Baby Blue
        friendlyColors.add(new Color(255, 192, 203));   // Pink
        friendlyColors.add(new Color(211, 211, 211));   // Light Grey
        friendlyColors.add(new Color(173, 216, 230));   // Light Sky Blue
        friendlyColors.add(new Color(255, 235, 205));   // Blanched Almond
        friendlyColors.add(new Color(221, 160, 221));   // Plum
        friendlyColors.add(new Color(255, 250, 205));   // Lemon Chiffon
        friendlyColors.add(new Color(255, 192, 203));   // Pink
        friendlyColors.add(new Color(173, 216, 230));   // Light Sky Blue
        friendlyColors.add(new Color(221, 160, 221));   // Plum
    }
    private void init()
    {
        valueList = new ArrayList<>();
        timeList = new ArrayList<>();
        createColors();
        for (int i = 0; i < cTimeSeries.size(); i++)
        {
            TimedVariable tv = cTimeSeries.sampleAt(i);

            // Don't plot values flagged for deletion.
            if (tv == null || VarFlags.mustDelete(tv))
                continue;
            try
            {
                double value = tv.getDoubleValue();
                Date timestamp = tv.getTime();
                valueList.add(value);
                timeList.add(timestamp);

            }
            catch (NoConversionException e)
            {
             log.warn("Error reading double from: "+tv);
            }
        }
    }

    public int getSize()
    {
        return cTimeSeries.size();
    }
    public Date getTime(int index)
    {
        return timeList.get(index);
    }
    public double getValue(int index)
    {
        return valueList.get(index);
    }
    public String getName()
    {
        return cTimeSeries.getDisplayName();
    }
    public String getUnits()
    {
        return cTimeSeries.getUnitsAbbr();
    }

    public Color getColor()
    {
        return color;
    }
}
