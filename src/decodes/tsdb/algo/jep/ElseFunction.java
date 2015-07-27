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
 * This implements the else(result) function for the JEP parser when run inside
 * the ExpressionParserAlgorithm.
 * The function takes a single argument and uses lazy evaluation.
 * If the previous to the cond() function resulted in false, then expression is evaluated
 * and returned. Otherwise a parse exception is thrown.
 */
public class ElseFunction
	extends PostfixMathCommand
	implements CallbackEvaluationI
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
			throw new ParseException("else function requires 1 argument!");
		
		if (ctx.getLastConditionFailed())
		{
//Logger.instance().info("executing the else arg");
			return evaluator.eval(node.jjtGetChild(0));
		}
		else
		{
//Logger.instance().info("Else failed");
			return new Double(0.0);
	//throw new ParseException("Else failed");
		}
	}
}
