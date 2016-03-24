package decodes.tsdb.algo;

import org.python.util.PythonInterpreter; 
import org.python.core.*; 

import decodes.tsdb.BadTimeSeriesException;

public class PyTest
{
	public static void main(String[] args) throws PyException
	{
		PythonInterpreter interp = new PythonInterpreter();

		System.out.println("Hello, brave new world");
		interp.exec("import sys");
		interp.exec("print sys");

		interp.set("a", new PyInteger(42));
		interp.exec("print a");
		interp.exec("x = 2+2");
		PyObject x = interp.get("x");

		System.out.println("x: " + x);
		
		interp.exec("from decodes.tsdb.algo import PyTest");
		interp.exec("print PyTest.get123()");
		
		String linesep = System.getProperty("line.separator");
		interp.exec("from decodes.tsdb import BadTimeSeriesException");
		String script = 
			  "try:" + linesep
			+ "    print PyTest.getValue()" + linesep
			+ "except BadTimeSeriesException as myex:" + linesep
			+ "    print 'Got an exception: ', myex" + linesep
			+ "" + linesep;
		System.out.println("Executing script:" + linesep + script);
		interp.exec(script);
	}
	
	public static int get123()
	{
		return 123;
	}
	
	public static double getValue()
		throws BadTimeSeriesException
	{
		throw new BadTimeSeriesException("Exception Message Here");
	}
}
