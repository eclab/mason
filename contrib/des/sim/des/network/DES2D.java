/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.network;

import sim.field.continuous.*;
import sim.field.network.*;
import sim.portrayal.network.*;
import sim.util.*;
import sim.des.*;

public class DES2D
	{
	Continuous2D nodes;
	Network edges;
	SpatialNetwork2D field;
		
	public DES2D(double discretization, double width, double height)
		{
		nodes = new Continuous2D(discretization, width, height);
		edges = new Network(true);
		field = new SpatialNetwork2D(nodes, edges);
		}

	public DES2D(double width, double height)
		{
		this(width, Math.min(width, height), height);
		}
		
	public void add(Object obj, Double2D location)
		{
		nodes.setObjectLocation(obj, location);
		}

	public void add(Object obj, double x, double y)
		{
		add(obj, new Double2D(x, y));
		}

	public void connect(Provider provider, Receiver receiver)
		{
		edges.addEdge(new ResourceEdge(provider, receiver, edges));
		}
		
	public SpatialNetwork2D getField()
		{
		return field;
		}
	}