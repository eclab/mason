package sim.util;
import java.io.*;

/**
   A simple RuntimeException which wraps a deeper exception inside of it and
   prints it in a fashion similar to exceptions like InvocationTargetException.
   This exception is largely obviated by the 'cause' mechanism in Java 1.4
   Throwables -- but Java 1.3 doesn't have it and thus we need it.
*/


public class CausedRuntimeException extends RuntimeException
    {
    private Throwable target;
    String message;
        
    protected CausedRuntimeException() { /* Not allowed */ }
                
    public CausedRuntimeException(Throwable target)
        {
        super();
        this.target = target;
        message = "";
        }
                
    public CausedRuntimeException(Throwable target, String message)
        {
        super(message);
        this.target = target;
        this.message = message;
        }
        
    public void printStackTrace(PrintStream stream)
        {
        if (target == null)  // duh, need to print our own stack trace
            super.printStackTrace();
        else
            stream.println("CausedRuntimeException: " + message);
        stream.println("Caused By:");
        target.printStackTrace(stream);
        }

    public void printStackTrace(PrintWriter stream)
        {
        if (target == null)  // duh, need to print our own stack trace
            super.printStackTrace();
        else
            stream.println("CausedRuntimeException: " + message);
        stream.println("Caused By:");
        target.printStackTrace(stream);
        }
                
    public void printStackTrace() { printStackTrace(System.err); }
    }
