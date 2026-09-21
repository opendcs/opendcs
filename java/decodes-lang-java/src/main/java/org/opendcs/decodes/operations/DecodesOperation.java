package org.opendcs.decodes.operations;

import org.opendcs.decodes.exec.DecodesExecutionContext;

/**
 * DecodesOperations
 */
public interface DecodesOperation
{
    String operationName();
    int repeat();

    // execution method will be here later, right now we're just parsing the script.
    void execute(DecodesExecutionContext<?> context);
}
