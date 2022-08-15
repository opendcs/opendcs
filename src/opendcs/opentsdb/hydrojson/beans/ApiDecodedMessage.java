package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import ilex.var.TimedVariable;

public class ApiDecodedMessage
{
	private Date messageTime = null;
	private ArrayList<ApiLogMessage> logMessages = new ArrayList<ApiLogMessage>();
	private ArrayList<ApiDecodesTimeSeries> timeSeries = new ArrayList<ApiDecodesTimeSeries>();

	public ArrayList<ApiLogMessage> getLogMessages()
	{
		return logMessages;
	}

	public void setLogMessages(ArrayList<ApiLogMessage> logMessages)
	{
		this.logMessages = logMessages;
	}

	public Date getMessageTime()
	{
		return messageTime;
	}

	public void setMessageTime(Date messageTime)
	{
		this.messageTime = messageTime;
	}

	public ArrayList<ApiDecodesTimeSeries> getTimeSeries()
	{
		return timeSeries;
	}

	public void setTimeSeries(ArrayList<ApiDecodesTimeSeries> timeSeries)
	{
		this.timeSeries = timeSeries;
	}

	public void fillFromDecodes(DecodedMessage dm, ArrayList<DecodedSample> decodedSamples)
	{
		this.messageTime = dm.getMessageTime();
		for (decodes.decoder.TimeSeries ts : dm.getTimeSeriesArray())
		{
			ApiDecodesTimeSeries adts = new ApiDecodesTimeSeries();
			adts.setSensorNum(ts.getSensorNumber());
			adts.setSensorName(ts.getSensorName());
			adts.setUnits(ts.getUnits());
			for(int i=0; i<ts.size(); i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				ApiDecodesTSValue adtsv = new ApiDecodesTSValue();
				adtsv.setTime(tv.getTime());
				adtsv.setValue(tv.getStringValue());
				adts.getValues().add(adtsv);
				for(Iterator<DecodedSample> dsit = decodedSamples.iterator(); dsit.hasNext(); )
				{
					DecodedSample ds = dsit.next();
					if (ds.getTimeSeries() == ts && ds.getSample() == tv)
					{
						adtsv.setRawDataPosition(ds.getRawDataPosition());
						dsit.remove();
						break;
					}
				}
			}
			timeSeries.add(adts);
		}
		
	}

}
