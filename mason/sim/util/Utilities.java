/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.awt.*;
import javax.swing.*;

/** Various static utility methods. */

public class Utilities
    {
    /** Returns a filename guaranteed to end with the given ending. */
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }
                
    /** Does a repaint that is guaranteed to work (on some systems, plain repaint())
        fails if there's lots of updates going on as is the case in our simulator thread.  Notably,
        MacOS X 1.3.1 has problems. */
    public static void doEnsuredRepaint(final Component component)
        {
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                if (component!=null) component.repaint();
                }
            });
        }
                
    /** Schedule something to occur at some specified point in the future in the Swing Event thread. */
    public static Thread doLater(final long milliseconds, final Runnable doThis)
        {
        Thread thread = new Thread(new Runnable()
            {
            public void run()
                {
                try
                    {
                    Thread.sleep(milliseconds);
                    SwingUtilities.invokeAndWait(doThis);
                    }
                catch (InterruptedException e) { /* shouldn't happen -- we could do an invokeAndWait in case tho */ }
                catch (java.lang.reflect.InvocationTargetException e) { /* shouldn't happen */ }
                }
            });
        thread.start();
        return thread;
        }
    }
