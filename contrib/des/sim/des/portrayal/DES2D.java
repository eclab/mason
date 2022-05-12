/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.portrayal;

import sim.field.continuous.*;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.util.*;
import sim.des.*;

/**
	An extension of SpatialNetwork2D meant to make it easy to lay out a DES graph visually.
	All you need to do is create a DES2D, then add the objects to it one by one, with locations
	for them specified.  Then
	you connect them, either manually, or via connectAll(), which hooks up all the Providers
	to their Receivers.  Now you just hand the DES2D to a NetworkPortrayal2D and ContinuousPortrayal2D
	to draw the edges and nodes.
*/


public class DES2D extends SpatialNetwork2D
	{
    private static final long serialVersionUID = 1;
		
	public DES2D(double width, double height)
		{
		super(new Continuous2D(Math.min(width, height), width, height),
			  new Network(true));
		}
		
	public void add(Object obj, Double2D location)
		{
		((Continuous2D)field).setObjectLocation(obj, location);
		}

	public void add(Object obj, double x, double y)
		{
		add(obj, new Double2D(x, y));
		}

	public void connect(Provider provider, Receiver receiver)
		{
		network.addEdge(new ResourceEdge(provider, receiver, network));
		}
		
	public void connect(Provider provider)
		{
		for(Receiver receiver : provider.getReceivers())
			connect(provider, receiver);
		}
		
	public void connectAll()
		{
		Bag objs = ((Continuous2D)field).getAllObjects();
		for(int i = 0; i < objs.numObjs; i++)
			{
			Object obj = objs.objs[i];
			if (obj instanceof Provider)
				connect((Provider)obj);
			}
		}
		
	public void clear()
		{
		((Continuous2D)field).clear();
		network.clear();
		}
	
	public Continuous2D getNodes()
		{
		return ((Continuous2D)field);
		}
	
	public Network getEdges()
		{
		return network;
		}
	}