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
package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This implements the lookupMeta(location, varname) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes two arguments: location and variable name.
 */
public class LookupMetaFunction	extends PostfixMathCommand
{
	public static final String funcName = "lookupMeta";

	public LookupMetaFunction()
	{
		super();
		this.numberOfParameters = 2;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		String paramName = inStack.pop().toString();
		String locName = inStack.pop().toString();
		
		throw new ParseException(funcName + " not yet implemented");
	}
}
