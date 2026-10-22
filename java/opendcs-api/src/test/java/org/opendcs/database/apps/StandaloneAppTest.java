package org.opendcs.database.apps;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;

class StandaloneAppTest 
{

    @Test 
    void test_start_loop_stop() throws Exception
    {
        var app = new TestApp.Builder().withMessage("Test").build();

        app.start();
        
        Thread.sleep(400);
        app.stop();

        assertFalse(app.messages.isEmpty());
        assertTrue(app.messages.size() < 10); // don't need an exact count just that it didn't go wild.
    }

    @Test
    void test_wait_timeout() throws Exception
    {
        var app = new TestApp.Builder()
                             .withMessage("Test")
                             .withShutdownWait(400, TimeUnit.MILLISECONDS)
                             .withLoopWait(500)
                             .build();
        
        app.start();
        Thread.sleep(200); // let things get going

        assertFalse(app.stop());
    }


    static class TestApp extends StandaloneApp
    {
        private final String message;
        private final Stack<String> messages = new Stack<>();
        private final AtomicInteger messageIdx = new AtomicInteger(0);
        private final int loopWait;

        protected TestApp(Builder builder)
        {
            super(builder);
            this.message = builder.message;
            this.loopWait = builder.loopWait;
        }

        @Override
        public void init()
        {
            messages.push(message + messageIdx.getAndIncrement());
        }

        @Override
        public void loop()
        {
            messages.push(message + messageIdx.getAndIncrement());
            if (loopWait > 0)
            {
                try
                {
                    Thread.sleep(loopWait);
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }
        }

        static class Builder extends StandaloneApp.AppBuilder<TestApp,Builder>
        {
            String message;
            int loopWait;

            public Builder withMessage(String message)
            {
                this.message = message;
                return self();
            }

            public Builder withLoopWait(int loopWait)
            {
                this.loopWait = loopWait;
                return self();
            }

            @Override
            protected Builder self()
            {
                return this;
            }

            @Override
            public TestApp build() throws OpenDcsApplicationException
            {
                return new TestApp(this);
            }
            
        }
    }
}
