package jython;

import org.junit.jupiter.api.Test;
import org.python.core.PyCode;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import static org.junit.jupiter.api.Assertions.*;

public class SitePackageTest {
    @Test
    public void test_site_packages() throws Exception
    {
        try(PythonInterpreter python = new PythonInterpreter();)
        {
            PyCode testCode = python.compile("import test \r\ntest.say_hello()");
            PyObject result = python.eval(testCode);
    
            assertEquals("hello",result.asString());
        }
    }
}
