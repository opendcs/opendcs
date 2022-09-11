package ilex.util;

public class StdOutLogger extends Logger {

    StdOutLogger()
    {
        super("");
    }

    StdOutLogger(String procName)
    {
        super(procName);
    }

    @Override
    public void close()
    {
        // leave System.out alone        
    }

    /**
	* Logs a message.
	* @param priority the priority
	* @param text the formatted text
	*/
    @Override
    public void doLog(int priority, String text)
    {
        System.out.println(standardMessage(priority, text));        
    }
    
}
