/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.des.portrayal.*;
import sim.portrayal.simple.*;
import sim.portrayal.network.*;
import sim.portrayal.*;
import java.util.*;
import sim.display.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import sim.util.*;
import javax.swing.*;
import sim.portrayal.continuous.*;
import sim.field.network.*;
import sim.engine.*;

/**
   A Macro encapsulates a subgraph of DES objects.  This subgraph consists of objects of three types.
   First there are receivers that the outside world might send resources to.  Second there are
   providers that might provide resources to the outside world.  Third, there are the "middlemen"
   in the graph which do not interact with the outside world.  To add an outside-world receiver
   to the graph, call addReceiver().  To add an outside-world provider to the graph, call addProvider().
   To add a middleman, call add().  You are responsible for wiring your graph up internally.  Macro
   at present does not have a way to clone its subgraphs.
   
   <p>When a Macro is stepped, it simply steps all of its steppables in turn.  Named objects are only
   stepped if you have indicated that they should be so when adding them to the Macro.  
   You technically don't need to add objects which aren't
   going to be stepped at all, presuming you have wired them up yourself.
*/

public class Macro extends DESPortrayal implements Parented
	{
    private static final long serialVersionUID = 1;

    class Node
        {
        Parented object;
        boolean shouldStep;
        public Node(Parented obj, boolean b) { object = obj; shouldStep = b; }
        };
        
    ArrayList<Node> steppables = new ArrayList<>();
    ArrayList<Parented> everything = new ArrayList<>();
    ArrayList<Receiver> receivers = new ArrayList<>();
    ArrayList<Provider> providers = new ArrayList<>();
    DES2D field;

	public Macro() { }
	public Macro(String name) { setName(name); }

	public void setField(DES2D field) { this.field = field; }
	public DES2D getField() { return field; }

    /** Adds the object to the graph and indicates whether it should be stepped when Macro is stepped. */
    public boolean add(Parented obj, boolean step)
        {
        if (!everything.contains(obj))
            {
            everything.add(obj);
            obj.setParent(this);
            steppables.add(new Node(obj, step));
            return true;
            }
        else return false;
        }

    /** Adds the public (world-facing) receiver to the graph and indicates whether it should be stepped when Macro is stepped. */
    public boolean addReceiver(Receiver recv, boolean step)
        {
        if (!receivers.contains(recv))
            {
            receivers.add(recv);
            add(recv, step);
            return true;
            }
        else return false;
        }
                
    /** Adds the public (world-facing) provider to the graph and indicates whether it should be stepped when Macro is stepped. */
    public boolean addProvider(Provider prov, boolean step)
        {
        if (!providers.contains(prov))
            {
            providers.add(prov);
            add(prov, step);
            return true;
            }
        else return false;
        }
        
    /** Returns all receivers */
    public Receiver[] getReceivers() 
    	{ 
    	return receivers.toArray(new Receiver[receivers.size()]); 
    	}
        
    /** Returns the names of all receivers. */
    public String[] getReceiverNames() 
        { 
        Receiver[] recv = getReceivers();
        String[] retval = new String[recv.length];
        for(int i = 0; i < recv.length; i++)
            retval[i] = recv[i].getName();
        return retval;
        }
                
    /** Returns all providers */
    public Provider[] getProviders() 
    	{ 
    	return providers.toArray(new Provider[providers.size()]); 
    	}
                
    /** Returns the names of all providers. */
    public String[] getProviderNames() 
        { 
        Provider[] prov = getProviders();
        String[] retval = new String[prov.length];
        for(int i = 0; i < prov.length; i++)
            retval[i] = prov[i].getName();
        return retval;
        }
                
    /** Steps all registered objects in turn. */
    public void step(SimState state)
        {
        for(Node node : steppables)
            {
            if (node.shouldStep)
                {
                node.object.step(state);
                }
            }
        }
        
      	// Okay, all this code has one goal: to pop up a display associated with the macro when the user TRIPLE-clicks on it.
    boolean doMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper, MouseEvent event, DrawInfo2D range, int type)
    	{
			// This first chunk of code is mostly co-opted from MovablePortrayal2D
	        synchronized(guistate.state.schedule)
    	        {
				int id = event.getID();
				Point2D.Double objPos = ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).getObjectPosition(wrapper.getObject(), range);

				if (id == MouseEvent.MOUSE_CLICKED && event.getClickCount() >= 3 && objPos != null)			// We're looking for 3 clicks
					{
					Point2D originalMousePosition = event.getPoint();     
					Point2D originalObjectPosition = objPos;  

					// we need to determine if we were actually hit
					DrawInfo2D hitRange = new DrawInfo2D(range);
					Double2D scale = ((FieldPortrayal2D)(wrapper.getFieldPortrayal())).getScale(range);

					// this magic basically creates a rectangle representing the hittable region of the object
					// and a small pixel where the mouse clicked.
					hitRange.draw.x = originalObjectPosition.getX();
					hitRange.draw.y = originalObjectPosition.getY();
					hitRange.draw.width = scale.x;
					hitRange.draw.height = scale.y;
					hitRange.clip.x = originalMousePosition.getX();
					hitRange.clip.y = originalMousePosition.getY();
					hitRange.clip.width = 1;
					hitRange.clip.height = 1;
					hitRange.parent = range;
														
					if (hitObject(wrapper.getObject(), hitRange))
						{
						
					/// END chunk of code copted from MovablePortrayal2D
						
						// Now we know we've been triple-clicked on.  So Let's see if we can find a display associated with the macro
						Console console = ((Console)(guistate.controller));
						ArrayList list = console.getAllFrames();
						
						// Go through every frame registered with the console
						for(Object obj : list)
							{
							JFrame frame = (JFrame)obj;	
							
							// The Display2D is a child of the content pane.  So we go through the children
							synchronized(frame.getContentPane().getTreeLock())		// this is dumb, there's no contains child command
								{
								Component[]	components = frame.getContentPane().getComponents();
								for(int i = 0; i < components.length; i++)
									{
									if (components[i] instanceof Display2D)
										{
										Display2D display2D = (Display2D)components[i];

										// Okay we found a Display2D.  Now we go through its portrayals one by one, looking for a ContinuousPortrayal2D
										Iterator iter = display2D.portrayals.iterator();
										while (iter.hasNext())
											{
											Display2D.FieldPortrayal2DHolder p = (Display2D.FieldPortrayal2DHolder)(iter.next());
											if (p.portrayal instanceof ContinuousPortrayal2D)
												{
												ContinuousPortrayal2D c = (ContinuousPortrayal2D)(p.portrayal);
												
												// Okay we found a ContinuousPortrayal2D.  Is it portraying the field we're looking for?
												if (c != null && field != null && c.getField() == field.getNodes())  // GOT IT
													{
													
													// Great!  So let's bring that frame to the front and return true
													SwingUtilities.invokeLater(new Runnable()
														{
														public void run()
															{
															frame.toFront();
															frame.setVisible(true);
															}
														});
													return true;		// we processed the event
													}
												}
											}	
										}		
									}
								}
							}
						}
					}
				return false;			// we did NOT process the event
				}
	    	}
        
    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean hideName() { return true; }
	Object parent;
    public Object getParent() { return parent; }
    public void setParent(Object parent) { this.parent = parent; }    
    
    public void reset(SimState state) { }

    public SimplePortrayal2D buildDefaultPortrayal(double scale)
      {
      return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_PILL, getFillPaint(), getStrokePaint(), getStrokeWidth(), scale)
      	{
		public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper,
			MouseEvent event, DrawInfo2D range, int type)
			{
			return doMouseEvent(guistate, manipulating, wrapper, event, range, type);
			}
      	};
      }

	public ImagePortrayal2D buildDefaultImagePortrayal(ImageIcon icon, double scale)
      {
      return new ImagePortrayal2D(icon, scale)
      	{
		public boolean handleMouseEvent(GUIState guistate, Manipulating2D manipulating, LocationWrapper wrapper,
			MouseEvent event, DrawInfo2D range, int type)
			{
			return doMouseEvent(guistate, manipulating, wrapper, event, range, type);
			}
      	};
      }
    }
