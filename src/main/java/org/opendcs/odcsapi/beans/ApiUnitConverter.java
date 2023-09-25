package org.opendcs.odcsapi.beans;

public class ApiUnitConverter
{
	private Long ucId = null;
	private String fromAbbr = null;
	private String toAbbr = null;

	/** One of Constants.eucvt_none, eucvt_linear, eucvt_usgsstd, eucvt_poly5 */
	private String algorithm = "none";
	
	/** coefficients for use by algorithm code. */
	private Double a=null, b=null, c=null, d=null, e=null, f=null;

	public String getFromAbbr()
	{
		return fromAbbr;
	}

	public void setFromAbbr(String fromAbbr)
	{
		this.fromAbbr = fromAbbr;
	}

	public String getToAbbr()
	{
		return toAbbr;
	}

	public void setToAbbr(String toAbbr)
	{
		this.toAbbr = toAbbr;
	}

	public String getAlgorithm()
	{
		return algorithm;
	}

	public void setAlgorithm(String algorithm)
	{
		this.algorithm = algorithm;
	}

	public Double getA()
	{
		return a;
	}

	public void setA(Double a)
	{
		this.a = a;
	}

	public Double getB()
	{
		return b;
	}

	public void setB(Double b)
	{
		this.b = b;
	}

	public Double getC()
	{
		return c;
	}

	public void setC(Double c)
	{
		this.c = c;
	}

	public Double getD()
	{
		return d;
	}

	public void setD(Double d)
	{
		this.d = d;
	}

	public Double getE()
	{
		return e;
	}

	public void setE(Double e)
	{
		this.e = e;
	}

	public Double getF()
	{
		return f;
	}

	public void setF(Double f)
	{
		this.f = f;
	}

	public Long getUcId()
	{
		return ucId;
	}

	public void setUcId(Long ucId)
	{
		this.ucId = ucId;
	}


}
