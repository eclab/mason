/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.desexample;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.network.*;
import sim.des.*;
import sim.des.portrayal.*;
import java.awt.geom.*;
import java.awt.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;

public class DESExample extends SimState
    {
    private static final long serialVersionUID = 1;

    public DES2D field = new DES2D(100, 100);
    public DES2D macroField = new DES2D(100, 100);
    
    public DESExample(long seed)
        {
        super(seed);
        }
              
    public Macro macro;
    
    public Lock lock1;
    public Lock lock2;
                
    public void start()
        {
        super.start();
        
        // Make a typical resource
    	Money quatloos = new Money("Q", 0);
        
        // Add some sources
        Source source1 = new Source(this, quatloos)
        	{
			public SimplePortrayal2D buildDefaultPortrayal(double scale)
				{
				return new ShapePortrayal2D(
					ShapePortrayal2D.POLY_SQUARE,
					Color.BLUE, Color.RED, 2.0, scale);
				}
        	};
        source1.setRate(1.0, false);
        source1.setProduction(1.1);
        source1.setAutoSchedules(true);
        // source1 needs to be added to the schedule ONCE -- thereafter it'll operate via autoscheduling
        schedule.scheduleOnce(Schedule.EPOCH, source1);
        	
        Source source2 = new Source(this, quatloos);
        source2.setRate(1.1, false);
        source2.setProduction(0.7);
        source2.setAutoSchedules(true);
        // source1 needs to be added to the schedule ONCE -- thereafter it'll operate via autoscheduling
        schedule.scheduleOnce(Schedule.EPOCH, source2);
        
        Pool pool = new Pool(new CountableResource("Locks", 20), 20);
        pool.setName("Blah");
        lock1 = new Lock(this, quatloos, pool);
        //lock1.setFillPaint(Color.GREEN);			// I have moved this to DESExampleWithUI.setupPortrayals() so it doesn't fire up the GUI when you run headless
        lock2 = new Lock(this, quatloos, pool);
        Unlock unlock = new Unlock(this, quatloos, pool);
        
        // Add a Simple Delay
        SimpleDelay simpleDelay = new SimpleDelay(this, 24.3, quatloos);
        simpleDelay.setCapacity(50);
        // FIME: at present simple delays autoschedule by default
        // but sources do not, and simple delays don't need to be added
        // to the schedule.  This is a bit confusing.

        // Add a Sink
        Sink sink = new Sink(this, quatloos);
        sink.setImage("images/factory.png", true);
        
        // Hook them up
        source1.addReceiver(lock1);
        source2.addReceiver(lock2);
        lock1.addReceiver(simpleDelay);
        lock2.addReceiver(simpleDelay);
        simpleDelay.addReceiver(unlock);
        unlock.addReceiver(sink);
        
        // Add A Macro
     	macro = new Macro("My Awesome Macro");
        macro.addProvider(source1, false);
        macro.addProvider(source2, false);
        
        // Set up our network for display purposes
        field = new DES2D(100, 100);
        //field.add(source1, 20, 40);
        //field.add(source2, 20, 60);
        field.add(macro, 20, 50);
        field.add(lock1, 35, 40);
        field.add(lock2, 35, 60);
        field.add(simpleDelay, 50, 50);
        field.add(unlock, 65, 50);
        field.add(sink, 80, 50);
        
        // Connect all objects with edges
        field.connectAll();
        
        // Let's make another display.  This has to be improved.
        // Maybe we could store the location information in the Macro proper,
        // and then make a method which builds a DES2D around the Macro
        macroField.add(source1, 20, 40);
        macroField.add(source2, 20, 60);
        macro.setField(macroField);

        /*
        // Alternatively we could connect the objects individually
        // Here are some being explicitly connected with edges
        field.connect(source1, lock1);
        field.connect(source2, lock2);
        field.connect(simpleDelay, unlock);
        // Here are some where I'm connecting the provider to all his receivers
        field.connect(lock1);
        field.connect(lock2);
        field.connect(unlock);
        */
        }
        
    public static void main(String[] args)
        {
        doLoop(DESExample.class, args);
        System.exit(0);
        }    
    }
