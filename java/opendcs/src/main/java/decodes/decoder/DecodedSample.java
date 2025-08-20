package decodes.decoder;

import ilex.var.TimedVariable;

/**
 * Encapsulates a single data sample decoded from a message.
 * It stores:
 * <ul>
 *   <li>The operation that did the decoding</li>
 *   <li>The position of the raw data within the message</li>
 *   <li>The timed variable that was decoded</li>
 *   <li>The time series into which data was placed</li>
 * </ul>
 * @author mmaloney Mike Maloney, Cove Software, LLC
 *
 */
public class DecodedSample
{
	/** Decodes Operation that decoded this sample */
	private DecodesOperation decodesOperation = null;
	
	/** The position of the raw data within the message */
	private TokenPosition rawDataPosition = null;
	
	/** The decoded data sample */
	private TimedVariable sample = null;
	
	/** The time series into which the sample was placed. */
	private TimeSeries timeSeries = null;

	public DecodedSample(DecodesOperation decodesOperation, 
		int rawDataStart, int rawDataEnd,
		TimedVariable sample, TimeSeries timeSeries)
	{
		super();
		this.decodesOperation = decodesOperation;
		this.rawDataPosition = new TokenPosition(rawDataStart, rawDataEnd);
		this.sample = sample;
		this.timeSeries = timeSeries;
	}

	public TokenPosition getRawDataPosition()
	{
		return rawDataPosition;
	}

	public TimedVariable getSample()
	{
		return sample;
	}

	public TimeSeries getTimeSeries()
	{
		return timeSeries;
	}

	public DecodesOperation getDecodesOperation()
	{
		return decodesOperation;
	}

}
