/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package jython;

import decodes.tsdb.DbCompException;
import org.junit.jupiter.api.Test;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;

public class SystemExitTest 
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
                    log.info("test script exited with system exit and zero exit code, type: {} value: {}", pe.type, pe.value);
                    return;
                }
            }
            log.atWarn().setCause(ex).log("Error executing test script.");
            throw ex;
        }
    }
}
