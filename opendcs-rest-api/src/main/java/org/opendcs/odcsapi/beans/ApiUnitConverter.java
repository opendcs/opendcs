/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.beans;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a unit conversion configuration, defining the source and target units, algorithm, and coefficients.")
public final class ApiUnitConverter
{
	@Schema(description = "Unique numeric identifier for the unit converter.", example = "3689")
	private Long ucId = null;

	@Schema(description = "Abbreviation of the source unit.", example = "m^3/s")
	private String fromAbbr = null;

	@Schema(description = "Abbreviation of the target unit.", example = "cms")
	private String toAbbr = null;

	/**
	 * One of Constants.eucvt_none, eucvt_linear, eucvt_usgsstd, eucvt_poly5
	 */
	@Schema(description = "Algorithm used for the unit conversion. One of: Constants.eucvt_none," +
			" eucvt_linear, eucvt_usgsstd, eucvt_poly5.", example = "none")
	private String algorithm = "none";

	/**
	 * coefficients for use by algorithm code.
	 */
	@Schema(description = "Coefficient 'a' for the conversion algorithm.", example = "1.0")
	private Double a = null;

	@Schema(description = "Coefficient 'b' for the conversion algorithm.", example = "2.0")
	private Double b = null;

	@Schema(description = "Coefficient 'c' for the conversion algorithm.", example = "3.0")
	private Double c = null;

	@Schema(description = "Coefficient 'd' for the conversion algorithm.", example = "4.0")
	private Double d = null;

	@Schema(description = "Coefficient 'e' for the conversion algorithm.", example = "5.0")
	private Double e = null;

	@Schema(description = "Coefficient 'f' for the conversion algorithm.", example = "6.0")
	private Double f = null;

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
