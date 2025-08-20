package decodes.tsdb.algo.jep;

import ilex.util.Logger;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * This function sets the gotoLabel in the context to whatever the argument is.
 * During script execution the reset() method in the context will clear gotoLabel.
 * The script executer checks for a goto setting after each expression.
 * The return value is 0.
 */
public class LogFunction
	extends PostfixMathCommand
{
	private JepContext ctx;
	private int debugLevel;
	
	public LogFunction(JepContext ctx, int debugLevel)
	{
		super();
		this.ctx = ctx;
		this.debugLevel = debugLevel;
		this.numberOfParameters = 1;
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);
		String logMsg = inStack.pop().toString();
		
		switch(debugLevel)
		{
		case Logger.E_DEBUG3: ctx.getAlgo().debug3(logMsg); break;
		case Logger.E_DEBUG2: ctx.getAlgo().debug2(logMsg); break;
		case Logger.E_DEBUG1: ctx.getAlgo().debug1(logMsg); break;
		case Logger.E_INFORMATION: ctx.getAlgo().info(logMsg); break;
		case Logger.E_WARNING:
		default:
			ctx.getAlgo().warning(logMsg);
		}
		inStack.push(Double.valueOf(0.0));
	}
}
