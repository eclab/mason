/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.des.portrayal.*;
import sim.portrayal.simple.*;
import sim.portrayal.*;
import java.util.*;

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
    public Receiver[] getReceivers() { return receivers.toArray(new Receiver[receivers.size()]); }
        
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
    public Provider[] getProviders() { return providers.toArray(new Provider[providers.size()]); }
                
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
      return new ShapePortrayal2D(ShapePortrayal2D.SHAPE_PILL, 
      getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
      }
    }
