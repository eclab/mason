/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.serengeti;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.*;
import sim.portrayal.simple.*;
import sim.portrayal.SimplePortrayal2D;

import ec.*;
import ec.util.*;
import ec.gp.*;
import java.io.*;

public class SerengetiWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
	public static GPIndividual ind;
	
    public static void main(String[] args) throws Exception
        {
        // Build ECJ
        ParameterDatabase base = Evolve.loadParameterDatabase(args);
		EvolutionState evolutionState = Evolve.initialize(base, 0);		// need to change the offset later FIXME
		evolutionState.startFresh();
		ind = (GPIndividual)(evolutionState.population.subpops.get(0).individuals.get(0));
		
		String best = base.getString(new Parameter("best-individual"), null);
		if (best == null)  // uh oh
			throw new RuntimeException("Sean, you are a dummy");
			
		ind.readIndividual(evolutionState, new LineNumberReader(new FileReader(new File(best))));
			
		
        // Load the individual
        
        new SerengetiWithUI().createController();  // randomizes by currentTimeMillis
        }

    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    ContinuousPortrayal2D fieldPortrayal = new ContinuousPortrayal2D();
            
    public SerengetiWithUI()
        {
        super(new Serengeti(System.currentTimeMillis()));
        }
    
    public SerengetiWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName() { return "Serengeti"; }

    public void start()
        {
        super.start();
        setupPortrayals();
        }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        Serengeti flock = (Serengeti)state;

        fieldPortrayal.setField(flock.field);
        fieldPortrayal.setPortrayalForClass(Lion.class, new OvalPortrayal2D(Color.red));
        fieldPortrayal.setPortrayalForClass(Gazelle.class, new OvalPortrayal2D(Color.blue));
                            
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display2D(500,500,this);
        display.setBackdrop(Color.black);


        displayFrame = display.createFrame();
        displayFrame.setTitle("Serengeti");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
                
        display.attach( fieldPortrayal, "Grasslands!" );
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }
    }
