package decodes.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TimeSeriesChart is used to generate a time-series plot
 *
 * Design goals:
 *  1- support simple time-series plotting using TimeSeriesLine(s) as input
 *  2- hide implementation (using jfreechart)
 */
public class TimeSeriesChart
{

    protected final Map<String, XYLineAndShapeRenderer> rendererMap = new HashMap<>();

    private final Map<String, List<TimeSeriesLine>> lineMap = new HashMap<>();
    private final Map<String, TimeSeriesCollection> timeSeriesCollectionMap = new HashMap<>();
    String title,xAxisText, yAxisText;
    public TimeSeriesChart(String title,String xAxisText, String yAxisText)
    {
        this.title = title;
        this.xAxisText = xAxisText;
        this.yAxisText = yAxisText;
    }
    private static Font axisFont() {
        return new Font("Segoe UI", Font.PLAIN, 12);
    }

    private static Font titleFont()  {
        return new Font("Segoe UI", Font.BOLD, 12);
    }

    public void addLine(TimeSeriesLine line) throws ParseException
    {
        TimeSeries newMember = new TimeSeries(line.getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        for (int i = 0; i < line.getSize(); i++)
        {
            Date dt = line.getTime(i);
            newMember.add(new Second(dt), line.getValue(i));
        }

        if (!timeSeriesCollectionMap.containsKey(line.getUnits()))
        {
            timeSeriesCollectionMap.put(line.getUnits(), new TimeSeriesCollection());
            lineMap.put(line.getUnits(), new ArrayList<>());
        }

        timeSeriesCollectionMap.get(line.getUnits()).addSeries(newMember);
        lineMap.get(line.getUnits()).add(line);
    }

    public JPanel generateChart() {
        CombinedDomainXYPlot plot = createTimeSeriesPlot();
        ChartPanel chart = new ChartPanel(new JFreeChart(title, plot));
        addChartFeatures(chart);
        return chart;
    }
    private void addChartFeatures(ChartPanel chart) {
        chart.setMouseZoomable(false);
        chart.setMouseWheelEnabled(true);
        chart.setDomainZoomable(true);
        chart.setRangeZoomable(true);

        //setChartToolTip(chart);
    }

    private void setChartToolTip(ChartPanel chart) {
        XYToolTipGenerator xyToolTipGenerator = (dataset, series, item) -> {
            Number x1 = dataset.getX(series, item);
            Number y1 = dataset.getY(series, item);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(String.format("<html><p style='color:#0000ff;'>%s</p>", dataset.getSeriesKey(series)));
            stringBuilder.append(String.format("X:'%s'<br/>", new Date(x1.longValue())));
            stringBuilder.append(String.format("Y:'%s'", y1.toString()));
            stringBuilder.append("</html>");
            return stringBuilder.toString();
        };


        int[] index = {0};
        for (Map.Entry<String, XYLineAndShapeRenderer> entry : rendererMap.entrySet())
        {
            String k = entry.getKey();
            XYLineAndShapeRenderer v = entry.getValue();
            XYLineAndShapeRenderer renderer = ((XYLineAndShapeRenderer) chart.getChart().getXYPlot().getRenderer(index[0]));
            renderer.setToolTipGenerator(xyToolTipGenerator);
        }

        chart.setDismissDelay(Integer.MAX_VALUE);
    }


    private CombinedDomainXYPlot createTimeSeriesPlot() {
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new DateAxis("Time"));


        int[] index ={0};
        
        timeSeriesCollectionMap.forEach((units, tsCollection) -> {
            XYPlot plot = new XYPlot();
            plot.setDomainPannable(true);
            plot.setRangePannable(true);
            rendererMap.put(units, new XYLineAndShapeRenderer(true, false));
            XYLineAndShapeRenderer renderer = rendererMap.get(units);
            plot.setDataset(index[0], timeSeriesCollectionMap.get(units));
            plot.setRenderer(index[0], renderer);

            DateAxis domainAxis = new DateAxis(xAxisText);
            domainAxis.setTickLabelFont(axisFont());
            plot.setDomainAxis(domainAxis);

            NumberAxis rangeAxis = new NumberAxis(yAxisText);
            rangeAxis.setTickLabelFont(axisFont());
            plot.setRangeAxis(index[0], rangeAxis);

            plot.mapDatasetToDomainAxis(index[0], 0);
            plot.mapDatasetToRangeAxis(index[0], 0);

            List<TimeSeriesLine> linesForRange = lineMap.get(units);
            for (int j = 0; j < linesForRange.size(); j++) {
                TimeSeriesLine currentLine = linesForRange.get(j);
                plot.getRenderer(index[0]).setSeriesStroke(j, new BasicStroke(1.0f));
                if (currentLine.getColor() != null)
                {
                    plot.getRenderer(index[0]).setSeriesPaint(j, currentLine.getColor());
                }
            }
            combinedPlot.add(plot,1);
            index[0]++;
        });


        return combinedPlot;
    }



}
