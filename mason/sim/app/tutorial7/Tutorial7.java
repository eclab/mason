/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial7;
import sim.engine.*;
import sim.field.grid.*;

public class Tutorial7 extends SimState
    {
    public SparseGrid3D flies;
    public DoubleGrid2D xProjection;
    public DoubleGrid2D yProjection;
    public DoubleGrid2D zProjection;

    int width = 30;
    int height = 30;
    int length = 30;
    public void setWidth(int val) { if (val > 0) width = val; }
    public int getWidth() { return width; }
    public void setHeight(int val) { if (val > 0) height = val; }
    public int getHeight() { return height; }
    public void setLength(int val) { if (val > 0) length = val; }
    public int getLength() { return length; }

    public Tutorial7(long seed)
        {
        super(seed);
        }

    public void start()
        {
        super.start();
        
        flies = new SparseGrid3D(width,height,length);
        xProjection = new DoubleGrid2D(height,length);
        yProjection = new DoubleGrid2D(width,length);
        zProjection = new DoubleGrid2D(width,height);

        // schedule the zero-er at ordering 0
        schedule.scheduleRepeating(new Steppable()
            {
            public void step(SimState state)
                {
                xProjection.setTo(0);
                yProjection.setTo(0);
                zProjection.setTo(0);
                }
                
            // because I am an anonymous nested subclass (see Tutorial 3)...
            static final long serialVersionUID = -4596371762755892330L;
            });

        // make some random flies at ordering 1
        for(int i=0; i<100;i++)
            {
            Fly fly = new Fly();
            flies.setObjectLocation(fly, random.nextInt(width), random.nextInt(height), random.nextInt(length));
            schedule.scheduleRepeating(Schedule.EPOCH,1,fly,1);
            }
        
        }

    public static void main(String[] args)
        {
        doLoop(Tutorial7.class, args);
        System.exit(0);
        }    

    // because I have an anonymous nested subclass (see Tutorial 3)...
    static final long serialVersionUID = -7776187839992045098L;
    }
