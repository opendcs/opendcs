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

import ilex.util.TextUtil;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.NumberFormat;

/**
 * Filter program to normalize legacy files in Alberta Loader format.
 * @author mmaloney
 *
 */
public class NormalizeAL
{
	public static final void main(String args[])
		throws Exception
	{
		LineNumberReader lnr = new LineNumberReader(
			new InputStreamReader(System.in));
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(3);
		nf.setMinimumFractionDigits(3);
		nf.setGroupingUsed(false);
		String line;
		while ((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.length() < 35)
				continue;
			String numField = line.substring(22, 30).trim();
			try
			{
				Double d = Double.parseDouble(numField);
				numField = nf.format(d);
				while(numField.length() > 8)
					numField = numField.substring(0, numField.length() - 1);
			}
			catch(NumberFormatException ex)
			{
				System.err.println("Bad value field on line " + lnr.getLineNumber()
					+ "'" + numField + "'");
				continue;
			}
//			// Remove leading zeros
//			if (numField.charAt(0) == '0')
//				while(numField.length() > 1 && numField.charAt(1) == '0')
//				numField.deleteCharAt(1);
//			// find the decimal point and truncate to 3 fractional digits.
//			int dp = numField.indexOf(".");
//			if (dp > 0 && numField.length() > dp+4)
//				numField.delete(dp+4, numField.length());
			
			// Rebuild the line
			StringBuilder outl = new StringBuilder();
			outl.append(line.substring(0, 22));
			outl.append(TextUtil.setLengthLeftJustify(numField, 8));
			outl.append(line.substring(30));
			System.out.println(outl.toString());
		}
	}
}
