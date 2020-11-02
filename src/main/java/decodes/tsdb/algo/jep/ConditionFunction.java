package decodes.tsdb.algo.jep;

import ilex.util.Logger;
import ilex.util.TextUtil;

import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.ParserVisitor;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This implements the cond(expr, result) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes two arguments and uses lazy evaluation.
 * The 'expr' argument is evaluated and if it is true (numeric non-zero, string value
 * of true or yes), then the second argument is evaluated and returned.
 * If 'expr' evaluates to false, then ParseException("Condition Failed") is returned, causing
 * the cond() function to return null.
 */
public class ConditionFunction
	extends PostfixMathCommand
	implements CallbackEvaluationI
{
	public static final String funcName = "cond";
	private JepContext ctx = null;

	public ConditionFunction(JepContext ctx)
	{
		super();
		this.numberOfParameters = 2;
		this.ctx = ctx;
	}
	
	@Override
	public boolean checkNumberOfParameters(int n)
	{
		return n == 2;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI evaluator) throws ParseException
	{
		if (!checkNumberOfParameters(node.jjtGetNumChildren()))
			throw new ParseException("cond syntax error. Usage: cond(cond-expression, true-expression)!");
	
		ctx.setLastStatementWasCond(true);
		
		// Evaluate the condition
		Object condResult = evaluator.eval(node.jjtGetChild(0));
		if (!isTrue(condResult))
		{
			ctx.setLastConditionFailed(true);
Logger.instance().info("condition is false, returning zero.");
			// Return zero and don't evaluate the second expression, which is probably
			// an assignment.
			return new Double(0.0);
		}
		else // evaluate & return value of second arg
			return evaluator.eval(node.jjtGetChild(1));
	}

	private boolean isTrue(Object result)
		throws ParseException
	{
		if (result instanceof Boolean)
			return (Boolean)result;
		else if (result instanceof Number)
		{
			Double d = ((Number)result).doubleValue();
			return d < -0.0000001 || d > 0.0000001;
		}
		else if (result instanceof String)
			return TextUtil.str2boolean((String)result);
		else
			throw new ParseException("Condition must result in boolean, string, or number!");
	}
}
