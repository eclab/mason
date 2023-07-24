/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import sim.portrayal.network.*;
import sim.portrayal.continuous.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.*;
import sim.field.network.*;
import sim.des.*;


public class MacroDisplay
    {
    NetworkPortrayal2D network = new NetworkPortrayal2D();
    ContinuousPortrayal2D continuous = new ContinuousPortrayal2D();
    Display2D display;
    JFrame frame;
        
    public void attachMacro(Macro macro, SimpleEdgePortrayal2D portrayal)
        {
        DES2D field = macro.getField();
        continuous.setField(field.getNodes());
        network.setField(field);
        network.setPortrayalForAll(portrayal);
        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();
        frame.setTitle(macro.getName());
        }

    public MacroDisplay(GUIState state, int width, int height, int macroNum)
        {
        display = new Display2D(width, height, state);
        display.setClipping(false);
        display.attach( network, "Connections" );
        display.attach( continuous, "Layout" );
        frame = display.createFrame();
        frame.setTitle("Macro " + macroNum);
        state.controller.registerFrame(frame);
        }
        
    }
        
