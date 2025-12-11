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

@Schema(description = "Represents a script format statement with metadata such as sequence number, label, and format.")
public final class ApiScriptFormatStatement
{
	@Schema(description = "The sequence number of this script format statement.", example = "0")
	private int sequenceNum = 0;

	@Schema(description = "The label associated with this script format statement.", example = "hg")
	private String label = null;

	@Schema(description = "The format of this script format statement.",
			example = "s(12,'#',getlabel),x,f(mint,a,3d' +-',1),32(w,c(N,skiphg),F(S,A,12d' +-:',1)), >GETLABEL")
	private String format = null;

	public int getSequenceNum()
	{
		return sequenceNum;
	}

	public void setSequenceNum(int sequenceNum)
	{
		this.sequenceNum = sequenceNum;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getFormat()
	{
		return format;
	}

	public void setFormat(String format)
	{
		this.format = format;
	}

}
