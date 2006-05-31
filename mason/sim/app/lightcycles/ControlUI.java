/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lightcycles;
import java.awt.*;
import java.awt.event.*;
import sim.display.*;

// This class is accurately named ControlUI instead of controlGUI because instead of allowing
// a user to interact with the simulation through graphical widgets, it simply adds keyboard 
// control to the display window.  This allows the user to control a cycle during the simulation.

public class ControlUI
    {
    // references to sim with ui, sim state, and selected cycle instance
    LightCyclesWithUI lcui;
    LightCycles lc;
    Cycle c;
    
    // for changing color of user-controlled cycle
    sim.portrayal.grid.SparseGridPortrayal2D cyclePortrayal;
    
    public ControlUI( LightCyclesWithUI nlc, sim.portrayal.grid.SparseGridPortrayal2D ncp )
        {
        
        lcui = nlc;
        lc = (LightCycles)lcui.state;
        cyclePortrayal = ncp;
        
        // Allows the display to receive input focus
        lcui.display.insideDisplay.setRequestFocusEnabled(true);
        
        try
            {
            initListeners();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }
    
    public void initListeners()
        {
        lcui.display.insideDisplay.addKeyListener( new KeyAdapter()
            {
            public void keyPressed( KeyEvent e )
                {
            
                // space = cycle the cycles
                if(e.getKeyCode() == KeyEvent.VK_SPACE)
                    {
                    for(int x=0;x<lc.cycleGrid.allObjects.numObjs;x++)
                        {
                        if(c != null)
                            {
                            // if one already selected, go next
                            if(c == ((Cycle)lc.cycleGrid.allObjects.objs[x]) && x != lc.cycleGrid.allObjects.numObjs-1)
                                {
                                cyclePortrayal.setPortrayalForObject(c, new sim.portrayal.simple.OvalPortrayal2D(Color.white));
                                c.cpu = true;
                                c = ((Cycle)lc.cycleGrid.allObjects.objs[x+1]);
                    
                                break;
                                }
                            // if last already selected, go first
                            else if(c == ((Cycle)lc.cycleGrid.allObjects.objs[x]) && x == lc.cycleGrid.allObjects.numObjs-1)
                                {
                                cyclePortrayal.setPortrayalForObject(c, new sim.portrayal.simple.OvalPortrayal2D(Color.white));
                                c.cpu = true;
                                c = ((Cycle)lc.cycleGrid.allObjects.objs[0]);
                    
                                break;
                                }
                            }
                        else
                            {
                            c = ((Cycle)lc.cycleGrid.allObjects.objs[x]);
                            break;
                            }
                        }
                    
                    // set controllage to new cycle
                    c.cpu = false;
                    cyclePortrayal.setPortrayalForObject(c, new sim.portrayal.simple.RectanglePortrayal2D(Color.green, 1.5));
                    
                    // Redraw immediately to display change in control; this is particularly necessary when paused.
                    lcui.controller.refresh();
                    } // end if spacebar
                
                // turn left
                else if( c!= null && e.getKeyCode() == KeyEvent.VK_LEFT)
                    {
                    if(c.dir == 1)
                        c.dir = 3;
                    else if(c.dir == 2)
                        c.dir = 4;
                    else if(c.dir == 3)
                        c.dir = 2;
                    else
                        c.dir = 1;
                    }
                
                // turn right
                else if( c!=null && e.getKeyCode() == KeyEvent.VK_RIGHT)
                    {
                    if(c.dir == 1)
                        c.dir = 4;
                    else if(c.dir == 2)
                        c.dir = 3;
                    else if(c.dir == 3)
                        c.dir = 1;
                    else
                        c.dir = 2;
                    }
                
                // PLAY CONTROLS
                // Play
                else if(e.getKeyCode() == KeyEvent.VK_A)
                    {
                    ((Console)(lcui.controller)).pressPlay();
                    }
                // Pause
                else if(e.getKeyCode() == KeyEvent.VK_S)
                    {
                    ((Console)(lcui.controller)).pressPause();
                    }
                // Stop
                else if(e.getKeyCode() == KeyEvent.VK_D)
                    {
                    ((Console)(lcui.controller)).pressStop();
                    }

                
                } // end keyPressed
            }); // end addKeyboardListener
        
        // Allows the user to give the display focus with the mouse
        lcui.display.insideDisplay.addMouseListener( new MouseAdapter()
            {
            public void getFocus()
                {
                lcui.display.insideDisplay.requestFocus();
                }
            
            public void mouseClicked(MouseEvent e)
                {
                getFocus();
                }
            
            // controversial!  but I think it's good.
            public void mouseEntered(MouseEvent e)
                {
                getFocus();
                }
        
            }); // end MouseListener
        } // end initListeners
    } // end ControlUI class
