package jython;

import org.junit.jupiter.api.Test;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

public class SitePackageTest {
    @Test
    public void test_site_packages() throws Exception
    {
        try(PythonInterpreter python = new PythonInterpreter();)
        {
            python.exec("import sys");
            String path = String.format("%s/python-packages",System.getProperty("opendcs.jarfile"));
            python.exec(String.format("sys.path.append(r\"%s\")",path));
            PyObject sysPath = python.eval("repr(sys.path)");
            System.err.println(sysPath.asString());
            python.exec("import requests");
            /* We're just making sure the above doesn't throw an exception */
        }
    }
}
