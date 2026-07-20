package decodes.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Verifies the fatal-uncaught-exception handler added for issue #1807.
 *
 * Three layers under test:
 *  - {@link RoutingScheduler#newFatalHandler} returns a handler that logs and
 *    invokes the supplied exit action.
 *  - {@link RoutingScheduler#installFatalHandler} installs that handler as the
 *    JVM default — this is the same code path {@code oneTimeInit()} runs.
 *  - A worker thread that throws an uncaught exception fires the installed
 *    handler and triggers the exit action with code 1.
 */
final class RoutingSchedulerTest
{
    @Test
    void newFatalHandler_invokesExitWithCodeOne()
    {
        AtomicInteger exitCode = new AtomicInteger(-1);
        Thread.UncaughtExceptionHandler handler =
            RoutingScheduler.newFatalHandler(exitCode::set);

        handler.uncaughtException(new Thread("worker-x"), new RuntimeException("boom"));

        assertEquals(1, exitCode.get(),
            "Fatal handler must call the exit action with code 1.");
    }

    @Test
    void installFatalHandler_setsDefaultAndFiresOnUncaughtException() throws Exception
    {
        AtomicReference<String> deadThreadName = new AtomicReference<>();
        AtomicReference<Throwable> deadCause = new AtomicReference<>();
        AtomicInteger exitCode = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);

        Thread.UncaughtExceptionHandler original =
            Thread.getDefaultUncaughtExceptionHandler();
        try
        {
            // Production code path: install via the same helper oneTimeInit() uses.
            RoutingScheduler.installFatalHandler(code ->
            {
                exitCode.set(code);
                latch.countDown();
            });

            Thread.UncaughtExceptionHandler installed =
                Thread.getDefaultUncaughtExceptionHandler();
            assertNotNull(installed,
                "installFatalHandler must register a JVM default uncaught-exception handler.");

            // Wrap the installed handler with a capture for thread name + cause so
            // we can verify the handler receives the actual thrown context, not
            // just that some handler ran.
            Thread.setDefaultUncaughtExceptionHandler((t, ex) ->
            {
                deadThreadName.set(t.getName());
                deadCause.set(ex);
                installed.uncaughtException(t, ex);
            });

            Thread bad = new Thread(() -> { throw new RuntimeException("boom"); }, "bad-worker");
            bad.start();

            assertTrue(latch.await(2, TimeUnit.SECONDS),
                "Default uncaught-exception handler did not fire within 2s.");
            assertEquals("bad-worker", deadThreadName.get());
            assertEquals("boom", deadCause.get().getMessage());
            assertEquals(1, exitCode.get(),
                "Installed handler must request exit code 1 when a worker thread dies.");
        }
        finally
        {
            Thread.setDefaultUncaughtExceptionHandler(original);
        }
    }
}
