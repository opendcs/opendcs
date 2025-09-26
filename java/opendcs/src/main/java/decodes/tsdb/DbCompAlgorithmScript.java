/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb;

import ilex.util.TextUtil;

/**
 * For Python Algorithms, this class will be associated with the DbCompAlgorithm
 * and will hold the information in the CP_ALGO_SCRIPT table.
 */
public class DbCompAlgorithmScript
{
	private DbCompAlgorithm parent = null;
	private String text = "";
	private ScriptType scriptType = ScriptType.Undefined;

	public DbCompAlgorithmScript(DbCompAlgorithm parent, ScriptType scriptType)
	{
		super();
		this.parent = parent;
		this.scriptType = scriptType;
	}

	public String getText() { return text; }

	public void addToText(String block)
	{
		if (text == null || text.length() == 0)
			text = block;
		else
			text = text + block;
	}

	public ScriptType getScriptType()
	{
		return scriptType;
	}

	public DbCompAlgorithm getParent()
	{
		return parent;
	}

	public DbCompAlgorithmScript copy(DbCompAlgorithm newAlgo)
	{
		DbCompAlgorithmScript ret = new DbCompAlgorithmScript(newAlgo, this.scriptType);
		ret.text = this.text;
		return ret;
	}

	@Override
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof DbCompAlgorithmScript))
			return false;
		DbCompAlgorithmScript rhss = (DbCompAlgorithmScript)rhs;
		return scriptType == rhss.scriptType && TextUtil.strEqual(text, rhss.text);
	}
}
