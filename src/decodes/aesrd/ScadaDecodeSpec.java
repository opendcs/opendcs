package decodes.aesrd;

import java.util.StringTokenizer;

import decodes.tsdb.NoSuchObjectException;

public class ScadaDecodeSpec
{
	/** Label in the scdalst.in file */
	public String label = null;
	
	public String newleafSite = null;
	
	public String sensorCodes[] = null;
	
	/**
	 * Construct a ScadaDecodeSpec from a line read from the scdalst.in file.
	 * Throw NoSuchObjectException if parse error.
	 * @param scdalst_line
	 */
	public ScadaDecodeSpec(String scdalst_line)
		throws NoSuchObjectException
	{
		if (scdalst_line.length() < 38)
			throw new NoSuchObjectException("line too short");
		if (!Character.isLetter(scdalst_line.charAt(0)))
			throw new NoSuchObjectException("First char must be a letter.");
		this.label = scdalst_line.substring(0, 21).trim();
		this.newleafSite = scdalst_line.substring(26, 36).trim();
		int numSensors = (int)scdalst_line.charAt(36) - (int)'0';
		if (numSensors <=0 || numSensors > 9)
			throw new NoSuchObjectException("Invalid numSensors '"
				+ scdalst_line.charAt(36) + "'");
		sensorCodes = new String[numSensors];
		StringTokenizer st = new StringTokenizer(scdalst_line.substring(37));
		for(int idx = 0; idx < numSensors; idx++)
		{
			String code = st.hasMoreTokens() ? st.nextToken() : null;
			if (code != null && code.equals("XX"))
				code = null;
			sensorCodes[idx] = code;
		}
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(label + "," + newleafSite + "," + sensorCodes.length);
		for(int idx = 0; idx < sensorCodes.length; idx++)
			sb.append("," + sensorCodes[idx]);
		return sb.toString();
	}
	
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getNewleafSite()
	{
		return newleafSite;
	}

	public void setNewleafSite(String newleafSite)
	{
		this.newleafSite = newleafSite;
	}

	public String[] getSensorCodes()
	{
		return sensorCodes;
	}

	public void setSensorCodes(String[] sensorCodes)
	{
		this.sensorCodes = sensorCodes;
	}

}
