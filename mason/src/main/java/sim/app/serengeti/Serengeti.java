/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.serengeti;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class Serengeti extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D field;
    public double width = 15;
    public double height = 15;
    public int numLions = 4;
    public double gazelleJump = 3.0;
    public double lionJump = 1.0;
    
    public Bag lions = new Bag();
    public Gazelle gazelle;
    public SerengetiEC problem = new SerengetiEC();
	public Lion currentLion;
	int tnum;
    
    public int getNumLions() { return numLions; }
    public void setNumLions(int val) { if (val >= 1) numLions = val; }
    public double getWidth() { return width; }
    public void setWidth(double val) { if (val > 0) width = val; }
    public double getHeight() { return height; }
    public void setHeight(double val) { if (val > 0) height = val; }
    public double getLionJump() { return lionJump; }
    public void setLionJump(double val) { lionJump = val; }
    public double getGazelleJump() { return gazelleJump; }
    public void setGazelleJump(double val) { gazelleJump = val; }
        
    /** Creates a Serengeti simulation with the given random number seed. */
    public Serengeti(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();
        
        // set up the Serengeti field.
        field = new Continuous2D(width,width,height);
        
        // make a bunch of Lions and schedule 'em.
        lions.clear();
        for(int x=0;x<numLions;x++)
            {
            Lion lion = new Lion();
            lion.number = x;
            lions.add(lion);
            Double2D location = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
            field.setObjectLocation(lion, location);
            lion.last = location;
            schedule.scheduleRepeating(lion, 1, 1.0);
            }
        
        // Add the Gazelle
        gazelle = new Gazelle();
        Double2D location = new Double2D(random.nextDouble() * width, random.nextDouble() * height);
        field.setObjectLocation(gazelle, location);
        schedule.scheduleRepeating(gazelle, 0, 1.0);
        }

    public static void main(String[] args)
        {
        doLoop(Serengeti.class, args);
        System.exit(0);
        }    
    }
