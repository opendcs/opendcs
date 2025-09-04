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
import java.util.function.Supplier;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * This function sets the gotoLabel in the context to whatever the argument is.
 * During script execution the reset() method in the context will clear gotoLabel.
 * The script executer checks for a goto setting after each expression.
 * The return value is 0.
 */
public class LogFunction extends PostfixMathCommand
{
	private JepContext ctx;
	private Supplier<LoggingEventBuilder> logSupplier;

	public LogFunction(JepContext ctx, Supplier<LoggingEventBuilder> logSupplier)
	{
		super();
		this.ctx = ctx;
		this.logSupplier = logSupplier;
		this.numberOfParameters = 1;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		String logMsg = inStack.pop().toString();
		logSupplier.get().log(logMsg);
		inStack.push(Double.valueOf(0.0));
	}
}
