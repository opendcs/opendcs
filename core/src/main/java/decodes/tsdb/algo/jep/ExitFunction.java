package decodes.tsdb.algo.jep;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This implements the exit() function for the JEP parser when run inside
 * the ExpressionParserAlgorithm. It sets the exitCalled boolean inside the
 * parser context which stops execution.
 */
public class ExitFunction
	extends PostfixMathCommand
{
	public static final String funcName = "exit";
	
	private JepContext ctx = null;

	public ExitFunction(JepContext ctx)
	{
		super();
		this.numberOfParameters = 0;
		this.ctx = ctx;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		ctx.setExitCalled(true);
		
		inStack.push(Double.valueOf(0.0));
	}
}
