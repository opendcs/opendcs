package org.opendcs.fixtures.assertions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import opendcs.util.functional.ThrowingFunction;

public class Waiting
{
    public static boolean waitForResult(ThrowingFunction<Long, Boolean, Exception> task,
                                        long waitFor, TimeUnit waitForUnit,
                                        long checkEvery, TimeUnit checkEveryUnit) throws Exception
    {
        boolean ret = false;
        long start = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long interval = checkEveryUnit.toMillis(checkEvery);
        long waitLength = waitForUnit.toMillis(waitFor);
        do
        {
            now = System.currentTimeMillis();
            ret = task.accept(now);
            if(!ret)
            {
                try
                {
                    Thread.sleep(interval);
                }
                catch (InterruptedException ex)
                {
                /* do nothing, just begin loop again */
                }
            }
        }
        while(ret!=true && (now - start) < waitLength);
        return ret;
    }

    public static void assertResultWithinTimeFrame(ThrowingFunction<Long, Boolean, Exception> task,
                                        long waitFor, TimeUnit waitForUnit,
                                        long checkEvery, TimeUnit checkEveryUnit, String messageIfFailed) throws Exception
    {
        assertTrue(waitForResult(task, waitFor, waitForUnit, checkEvery, checkEveryUnit), messageIfFailed);
    }    
}
