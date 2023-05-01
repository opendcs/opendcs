package decodes.tsdb.algo;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import decodes.tsdb.BadTimeSeriesException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PyTest
{
	private static final Logger LOGGER = Logger.getLogger(PyTest.class.getName());

	@Test
	void explorePythonInterpreter()
	{
		try(PythonInterpreter interp = new PythonInterpreter())
		{
			interp.exec("import sys");
			interp.exec("print sys");
			interp.set("a", new PyInteger(42));
			interp.exec("print a");
			interp.exec("x = 2+2");
			PyObject x = interp.get("x");
			assertEquals(4, x.asInt(), "Error with basic python arithmetic resolution to Java object");
		}
	}

	@Test
	void explorePythonInterpreterJavaExceptionHandling()
	{
		try(PythonInterpreter interp = new PythonInterpreter())
		{
			interp.exec("from decodes.tsdb.algo import PyTest");
			interp.exec("print PyTest.get123()");
			interp.exec("from decodes.tsdb import BadTimeSeriesException");
			String linesep = System.getProperty("line.separator");
			String script = "try:" + linesep
					+ "    print PyTest.getValue()" + linesep
					+ "except BadTimeSeriesException as myex:" + linesep
					+ "    print 'Got an exception: ', myex" + linesep
					+ "" + linesep;
			assertDoesNotThrow(() -> interp.exec(script), "Internal Jython exception handling should catch thrown exception");
		}
	}

	@Test
	void testSomething()
	{
		try(PythonInterpreter interp = new PythonInterpreter())
		{
			String linesep = System.getProperty("line.separator");
			interp.set("value", new PyFloat(Math.PI));
			interp.exec("print value");

			LOGGER.info("Defining class AlgoParm and an instance called foo:");
			interp.exec("class AlgoParm:" + linesep
					+ "\tdef __init__(self, tsid, value='NV', qual=0x40000000):" + linesep
					+ "\t\tself.tsid = tsid" + linesep
					+ "\t\tself.value = value" + linesep
					+ "\t\tself.qual = qual" + linesep);
			interp.exec("foo = AlgoParm('abc123')" + linesep);
			PyObject foo = interp.get("foo");
			assertEquals("abc123", foo.__getattr__("tsid").asString());
			assertEquals("NV", foo.__getattr__("value").asString());
			assertEquals(0x40000000, foo.__getattr__("qual").asInt());
			interp.set("x", new PyFloat(Math.PI));
			interp.exec("foo.value = x");
			assertEquals(Math.PI, foo.__getattr__("value").asDouble(), 0.0);
		}
	}

	//Used internally jython - needs to remain public for interpreter access
	public static int get123()
	{
		return 123;
	}

	//Used internally jython - needs to remain public for interpreter access
	public static double getValue() throws BadTimeSeriesException
	{
		throw new BadTimeSeriesException("Exception Message Here");
	}
}
