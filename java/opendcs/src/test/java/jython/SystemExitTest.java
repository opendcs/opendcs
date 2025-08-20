package jython;

import decodes.tsdb.DbCompException;
import org.junit.jupiter.api.Test;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import static decodes.db.DecodesScript.logger;

public class SystemExitTest {
    private static final PyException sysexit0 = new PyException();

    @Test
    public void test_systemexit() throws Exception
    {

        try(PythonInterpreter python = new PythonInterpreter();)
        {
            python.exec("exit(0)");
        }
        catch(Exception ex)
        {
            if (ex instanceof PyException) {
                PyException pe = ((PyException) ex);
                // after https://github.com/jython/jython/blob/master/src/org/python/core/Py.java#L282
                if (pe.type == Py.SystemExit && PyException.isExceptionInstance(pe.value)
                    && ((PyObject)pe.value).__findattr__("code").asInt() == 0) {
                    logger.info("test script exited with system exit and zero exit code, type: " + pe.type + " value: " + pe.value);
                    return;
                }
            }
            String msg = "Error executing test script: " + ex;
            logger.warning(msg);
            throw ex;
        }
    }
}
