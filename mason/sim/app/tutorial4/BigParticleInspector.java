/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial4;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import sim.field.grid.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import sim.engine.*;
import sim.util.*;
import sim.display.*;

public class BigParticleInspector extends Inspector
    {
    public Inspector originalInspector;
    
    public BigParticleInspector(Inspector originalInspector,
        LocationWrapper wrapper,
        GUIState guiState)
        {
        this.originalInspector = originalInspector;
        
        // get info out of the wrapper
        SparseGridPortrayal2D gridportrayal = (SparseGridPortrayal2D) wrapper.getFieldPortrayal();
        // these are final so that we can use them in the anonymous inner class below...
        final SparseGrid2D grid = (SparseGrid2D)(gridportrayal.getField());
        final BigParticle particle = (BigParticle) wrapper.getObject();
        final SimState state = guiState.state;
        final Controller console = guiState.controller;  // The Console (it's a Controller subclass)
        
        // now let's add a Button
        Box box = new Box(BoxLayout.X_AXIS);
        JButton button = new JButton("Roll the Dice");
        box.add(button);
        box.add(Box.createGlue());

        // set up our inspector: keep the properties inspector around too
        setLayout(new BorderLayout());
        add(originalInspector, BorderLayout.CENTER);
        add(box, BorderLayout.NORTH);
        
        // set what the button does
        button.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                synchronized(state.schedule)
                    {
                    // randomize direction
                    particle.xdir = state.random.nextInt(3) - 1;
                    particle.ydir = state.random.nextInt(3) - 1;
                    
                    // randomize location
                    grid.setObjectLocation(particle,
                        new Int2D(state.random.nextInt(grid.getWidth()),
                            state.random.nextInt(grid.getHeight())));
                    
                    // repaint everything: console, inspectors, displays,
                    // everything that might be affected by randomization
                    console.refresh();
                    }
                }
            });
        }
        
    public void updateInspector()
        {
        originalInspector.updateInspector();
        }
    }
