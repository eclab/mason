/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.celegans;
import sim.portrayal3d.network.*;
import sim.portrayal3d.continuous.*;
import sim.display3d.*;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import sim.portrayal3d.simple.*;
import java.awt.*;

public class CelegansWithUI extends GUIState
    {
    public Display3D display;
    public Display3D neuronDisplay;
    public JFrame displayFrame;
    public JFrame neuronDisplayFrame;

    ContinuousPortrayal3D nodePortrayal = new ContinuousPortrayal3D();
    ContinuousPortrayal3D neuronPortrayal = new ContinuousPortrayal3D();
    NetworkPortrayal3D synapsePortrayal = new NetworkPortrayal3D();

    public static void main(String[] args)
        {
        new CelegansWithUI().createController();
        }

    public CelegansWithUI() { super(new Celegans( System.currentTimeMillis())); }
    public CelegansWithUI(SimState state) 
        { 
        super(state); 
        }

    public static String getName() { return "Caenorhabditis elegans"; }
    
    public Object getSimulationInspectedObject() { return state; }

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
        // display.destroySceneGraph();
        
        Celegans tut = (Celegans) state;
        
        nodePortrayal.setField( tut.cells );
        //LabelledPortrayal3D l = new LabelledPortrayal3D(new CellPortrayal(50), null);
        // l.setLabelGoesOnTop(true);           // sadly, doesn't work.  Java3D bugs are a pain.
        // nodePortrayal.setPortrayalForAll(new CircledPortrayal3D(l, 60f, true));
        nodePortrayal.setPortrayalForAll(new CellPortrayal(50));

        neuronPortrayal.setField( tut.neurons );
        neuronPortrayal.setPortrayalForAll(new SpherePortrayal3D());

        synapsePortrayal.setField( new SpatialNetwork3D(tut.neurons, tut.synapses) );
        SimpleEdgePortrayal3D sep = new SimpleEdgePortrayal3D(Color.red, Color.blue, Color.white);
        
        sep.setLabelScale(sep.getLabelScale()/4.0);
        synapsePortrayal.setPortrayalForAll(sep);

        display.createSceneGraph(); 
        display.reset();
        }

    public void init(Controller c)
        {
        super.init(c);

        // make the displayer
        display = new Display3D(600, 600,this);   
        display.attach( nodePortrayal, "Cells" );
        display.scale(1.0/40.0);
                
        // tell the Display3D to inspect everybody, but to only select a single cell.
        display.setSelectsAll(false, true);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Embryo");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        //              display.mSelectBehavior.setTolerance(10.0f);


        // make the neuron displayer
        neuronDisplay = new Display3D(400,400,this);   
        neuronDisplay.attach( neuronPortrayal, "Neurons" );
        neuronDisplay.attach( synapsePortrayal, "Synapses" );
        neuronDisplay.scale(1.0/40.0);
                
        // tell the Display3D to inspect everybody, but to only select a single cell.
        neuronDisplay.setSelectsAll(false, true);

        neuronDisplayFrame = neuronDisplay.createFrame();
        neuronDisplayFrame.setTitle("Synapses");
        c.registerFrame(neuronDisplayFrame);   // register the frame so it appears in the "Display" list
        //neuronDisplayFrame.setVisible(true);
        }

    public void quit()
        {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;

        if (neuronDisplayFrame!=null) neuronDisplayFrame.dispose();
        neuronDisplayFrame = null;
        neuronDisplay = null;
        }

    }
