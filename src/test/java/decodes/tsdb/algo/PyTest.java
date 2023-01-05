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
		
		System.out.println();
		System.out.println("Setting pi in environment with full double precision.");
		interp.set("value", new PyFloat(Math.PI));
		System.out.println("Printing value of pi from Python:");
		interp.exec("print value");
		
		System.out.println("\nDefining class AlgoParm and an instance called foo:");
		interp.exec("class AlgoParm:" + linesep
			+ "\tdef __init__(self, tsid, value='NV', qual=0x40000000):" + linesep
			+ "\t\tself.tsid = tsid" + linesep
			+ "\t\tself.value = value" + linesep
			+ "\t\tself.qual = qual" + linesep);
		interp.exec("foo = AlgoParm('abc123')" + linesep);

		System.out.println("Before setting, printing foo.value:\n");
		interp.exec("print foo.value");
		System.out.println("Setting from interpreter to PI");
		interp.set("x", new PyFloat(Math.PI));
		interp.exec("foo.value = x");
		System.out.println("Printing foo.vaue");
		interp.exec("print foo.value");

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
