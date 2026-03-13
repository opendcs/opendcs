## Test Logging

The logs for computation tests are stored in the path configured in the
`test-config/logback-test-child.xml` file under the `FileAppender` node.

To see the full error logs, check the temporary directory for the log files. These will 
include errors in the computation processes that do not make it to System out.

The default path is `/build/runs/[DB-Type]/tmp/tests-[computation-name].log`, for
example: `/build/runs/CWMS-Oracle/tmp/tests-compdepends_compproc.log` for the
CWMS-Oracle database and the `compdepends_compproc` computation test.