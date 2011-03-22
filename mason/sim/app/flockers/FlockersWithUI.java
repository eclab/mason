/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.flockers;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;
import sim.portrayal.SimplePortrayal2D;

public class FlockersWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    org.jfree.data.xy.XYSeries series;    // the data series we'll add to
    sim.util.media.chart.TimeSeriesChartGenerator chart;  // the charting facility

    public static void main(String[] args)
        {
        new FlockersWithUI().createController();  // randomizes by currentTimeMillis
        }

    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    ContinuousPortrayal2D flockersPortrayal = new ContinuousPortrayal2D();
        
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
    ContinuousPortrayal2D trailsPortrayal = new ContinuousPortrayal2D(); 
    
    public FlockersWithUI()
        {
        super(new Flockers(System.currentTimeMillis()));
        }
    
    public FlockersWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName() { return "Flockers"; }

    public void start()
        {
        super.start();
        setupPortrayals();



        chart.removeAllSeries();
        series = new org.jfree.data.xy.XYSeries(
            "Put a unique name for this series here so JFreeChart can hash with it",
            false);
        chart.addSeries(series, null);
        scheduleRepeatingImmediatelyAfter(new Steppable()
            {
            public void step(SimState state)
               {
               // at this stage we're adding data to our chart.  We
               // need an X value and a Y value.  Typically the X
               // value is the schedule's timestamp.  The Y value
               // is whatever data you're extracting from your 
               // simulation.  For purposes of illustration, let's
               // extract the number of steps from the schedule and
               // run it through a sin wave.
               
               double x = state.schedule.time(); 
               double y = Math.sin(state.schedule.getSteps()) * 10;
               
               // now add the data
               if (x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION)
					series.add(x, y, false);  // don't update automatically
					startTimer(state.schedule.getSteps(), 1000);  // once a second (1000 milliseconds)
               }
           });




        }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        Flockers flock = (Flockers)state;

        flockersPortrayal.setField(flock.flockers);
        // uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
        trailsPortrayal.setField(flock.flockers);
        
        // make the flockers random colors and four times their normal size (prettier)
        for(int x=0;x<flock.flockers.allObjects.numObjs;x++)
            {
            SimplePortrayal2D basic =       new TrailedPortrayal2D(
                this,
                new OrientedPortrayal2D(
                    new SimplePortrayal2D(), 0, 4.0,
                    new Color(      128 + state.random.nextInt(128),
                        128 + state.random.nextInt(128),
                        128 + state.random.nextInt(128)),
                    OrientedPortrayal2D.SHAPE_COMPASS),
                trailsPortrayal, 100);

            // note that the basic portrayal includes the TrailedPortrayal.  We'll add that to BOTH 
            // trails so it's sure to be selected even when moving.  The issue here is that MovablePortrayal2D
            // bypasses the selection mechanism, but then sends selection to just its own child portrayal.
            // but we need selection sent to both simple portrayals in in both field portrayals, even after
            // moving.  So we do this by simply having the TrailedPortrayal wrapped in both field portrayals.
            // It's okay because the TrailedPortrayal will only draw itself in the trailsPortrayal, which
            // we passed into its constructor.
                        
            flockersPortrayal.setPortrayalForObject(flock.flockers.allObjects.objs[x], 
                new AdjustablePortrayal2D(new MovablePortrayal2D(basic)));
            trailsPortrayal.setPortrayalForObject(flock.flockers.allObjects.objs[x], basic );
            }
        
        // update the size of the display appropriately.
        double w = flock.flockers.getWidth();
        double h = flock.flockers.getHeight();
        if (w == h)
            { display.insideDisplay.width = display.insideDisplay.height = 750; }
        else if (w > h)
            { display.insideDisplay.width = 750; display.insideDisplay.height = 750 * (h/w); }
        else if (w < h)
            { display.insideDisplay.height = 750; display.insideDisplay.width = 750 * (w/h); }
            
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(750,750,this,1);
        display.setBackdrop(Color.black);


        displayFrame = display.createFrame();
        displayFrame.setTitle("Flockers");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
        display.attach( trailsPortrayal, "Trails" );
                
        display.attach( flockersPortrayal, "Behold the Flock!" );



       chart = new sim.util.media.chart.TimeSeriesChartGenerator();
       chart.setTitle("Put the title of your chart here");
       chart.setRangeAxisLabel("Put the name of your charted series here");
       chart.setDomainAxisLabel("Time");
       JFrame frame = chart.createFrame(this);
       // perhaps you might move the chart to where you like.
       frame.setVisible(true);
       frame.pack();
       c.registerFrame(frame);
       

        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    Thread timer = null;
    public void startTimer(final long steps, final long milliseconds)
       {
       if (timer == null)
           timer= sim.util.gui.Utilities.doLater(milliseconds, new Runnable()
              {
              public void run()
                  {
                  if (chart!=null) chart.update(steps, true);
                  timer = null;  // reset the timer
                  }
              });
       }
    }
