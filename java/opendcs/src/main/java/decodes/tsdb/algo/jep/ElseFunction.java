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


import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This implements the else(result) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes a single argument and uses lazy evaluation.
 * If the previous to the cond() function resulted in false, then expression is evaluated
 * and returned. Otherwise a parse exception is thrown.
 */
public class ElseFunction extends PostfixMathCommand implements CallbackEvaluationI
{
	public static final String funcName = "else";
	private JepContext ctx = null;

	public ElseFunction(JepContext ctx)
	{
		super();
		this.numberOfParameters = 1;
		this.ctx = ctx;
	}
	
	@Override
	public boolean checkNumberOfParameters(int n)
	{
		return n == 1;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI evaluator) throws ParseException
	{
		if (!checkNumberOfParameters(node.jjtGetNumChildren()))
		{
			throw new ParseException("else function requires 1 argument!");
		}
		
		if (ctx.getLastConditionFailed())
		{
			return evaluator.eval(node.jjtGetChild(0));
		}
		else
		{
			return Double.valueOf(0.0);	
		}
	}
}
