/*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.aesrd;

import java.util.Date;

/**
 * Immutable bean holding a row resulting from the SCADA query
 * @author mmaloney
 */
public class ScadaQueryRow
	implements Comparable<ScadaQueryRow>
{
	private Date datetime;
	private String tag;
	private String data;
	private String quality;
	
	public ScadaQueryRow(Date datetime, String tag, String data, String quality)
	{
		super();
		this.datetime = datetime;
		this.tag = tag;
		this.data = data;
		this.quality = quality;
	}

	public Date getDatetime()
	{
		return datetime;
	}

	public String getTag()
	{
		return tag;
	}

	public String getData()
	{
		return data;
	}

	public String getQuality()
	{
		return quality;
	}

	@Override
	public int compareTo(ScadaQueryRow rhs)
	{
		int r = tag.compareTo(rhs.tag);
		if (r != 0)
			return r;
		r = datetime.compareTo(rhs.datetime);
		if (r != 0)
			return r;
		// Shouldn't have two values with same tag and date/time.
		r = quality.compareTo(rhs.quality);
		if (r != 0)
			return r;
		return data.compareTo(rhs.data);
	}
	
	public String toString()
	{
		return "ScadaQueryRow: " + datetime + ", " + tag + ", " + data + ", " + quality;
	}
}
