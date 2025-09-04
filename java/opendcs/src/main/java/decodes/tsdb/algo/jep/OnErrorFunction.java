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
 * This sets the onErrorLabel in the context and then returns 0.
 * Subsequently, if any expression results in an error, execution jumps to the
 * specified label.
 */
public class OnErrorFunction extends PostfixMathCommand
{
	public static final String funcName = "onError";
	private JepContext ctx;

	public OnErrorFunction(JepContext ctx)
	{
		super();
		this.ctx = ctx;
		this.numberOfParameters = 1;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		String label = inStack.pop().toString();
		ctx.setOnErrorLabel(label);
		inStack.push(Double.valueOf(0.0));
	}
}
