/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.wcss.tutorial01;
import sim.engine.*;

public class Students extends SimState
    {
    private static final long serialVersionUID = 1;

    public Students(long seed)
        {
        super(seed);
        }

    public void start()
        {
        super.start();
        }
        
    public static void main(String[] args)
        {
        doLoop(Students.class, args);
        System.exit(0);
        }    
    }
