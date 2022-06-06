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
import java.util.*;
import sim.field.*;

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

    public ResourceEdge connect(Provider provider, Receiver receiver)
        {
        ResourceEdge edge = new ResourceEdge(provider, receiver);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Provider provider, Receiver receiver, Object in)
        {
        ResourceEdge edge = new ResourceEdge(provider, provider, receiver, in);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Macro out, Provider provider, Receiver receiver)
        {
        ResourceEdge edge = new ResourceEdge(out, provider, receiver, receiver);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Macro out, Provider provider, Receiver receiver, Object in)
        {
        ResourceEdge edge = new ResourceEdge(out, provider, receiver, in);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Multi out, int portOut, Receiver receiver)
        {
        ResourceEdge edge = new ResourceEdge(out, out.getProvider(portOut), receiver, receiver);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Provider provider, Multi in, int portIn)
        {
        ResourceEdge edge = new ResourceEdge(provider, provider, in.getReceiver(portIn), in);
        network.addEdge(edge);
        return edge;
        }

    public ResourceEdge connect(Multi out, int portOut, Receiver receiver, Object in)
        {
        ResourceEdge edge = new ResourceEdge(out, out.getProvider(portOut), receiver, in);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Multi out, Provider multiProvider, Receiver receiver, Object in)
        {
        ResourceEdge edge = new ResourceEdge(out, multiProvider, receiver, in);
        network.addEdge(edge);
        return edge;
        }
 
    public ResourceEdge connect(Object out, Provider provider, Multi in, int portIn)
        {
        ResourceEdge edge = new ResourceEdge(out, provider, in.getReceiver(portIn), in);
        network.addEdge(edge);
        return edge;
        }
               
    public ResourceEdge connect(Multi out, int portOut, Multi in, int portIn)
        {
        ResourceEdge edge = new ResourceEdge(out, out.getProvider(portOut), in.getReceiver(portIn), in);
        network.addEdge(edge);
        return edge;
        }
 
    public ArrayList<ResourceEdge> connect(Provider provider)
        {
        ArrayList<ResourceEdge> ret = new ArrayList<>();
        for(Receiver receiver : provider.getReceivers())
        	{
        	Object obj = receiver;
        	while(obj != null && !((SparseField)field).exists(obj))	// maybe its parent is here?
        		{
        		if (obj instanceof Parented)
        			{
	        		obj = ((Parented)obj).getParent();
	        		}
	        	else
	        		{
	        		obj = null;
	        		}
        		}
        	if (obj != null)
        		{
        		if (obj instanceof Receiver)
	            	ret.add(connect(provider, (Receiver)obj));
	            else if (obj instanceof Macro)
	            	ret.add(connect(provider, receiver, (Macro) obj));
	            else if (obj instanceof Multi)
	            	ret.add(connect(provider, receiver, (Multi) obj));
	            }
            }
        return ret;
        }
                
    public ArrayList<ResourceEdge> connect(Multi multiProvider)
        {
        ArrayList<ResourceEdge> ret = new ArrayList<>();
        for(Provider provider : multiProvider.getProviders())
			{
			for(Receiver receiver : provider.getReceivers())
				{
        		Object obj = receiver;
				while(obj != null && !((SparseField)field).exists(obj))	// maybe its parent is here?
					{
					if (obj instanceof Parented)
						{
	        			obj = ((Parented)obj).getParent();
						}
					else
						{
						obj = null;
						}
					}
				if (obj != null)
					{
					if (obj instanceof Receiver)
						ret.add(connect(multiProvider, provider, (Receiver)obj, (Receiver)obj));
					else if (obj instanceof Macro)
						ret.add(connect(multiProvider, provider, receiver, (Macro) obj));
					else if (obj instanceof Multi)
						ret.add(connect(multiProvider, provider, receiver, (Multi) obj));
					}
				}
			}
		return ret;
		}
                
    public ArrayList<ResourceEdge> connect(Macro macroProvider)
        {
        ArrayList<ResourceEdge> ret = new ArrayList<>();
        for(Provider provider : macroProvider.getProviders())
			{
			for(Receiver receiver : provider.getReceivers())
				{
				Object obj = receiver;
				while(obj != null && !((SparseField)field).exists(obj))	// maybe its parent is here?
					{
					if (obj instanceof Parented)
						{
	        			obj = ((Parented)obj).getParent();
						}
					else
						{
						obj = null;
						}
					}
				if (obj != null)
					{
					if (obj instanceof Receiver)
						ret.add(connect(macroProvider, provider, (Receiver)obj, (Receiver)obj));
					else if (obj instanceof Macro)
						ret.add(connect(macroProvider, provider, receiver, (Macro) obj));
					else if (obj instanceof Multi)
						ret.add(connect(macroProvider, provider, receiver, (Multi) obj));
					}
				}
			}
		return ret;
		}
                
   public ArrayList<ResourceEdge> connectAll()
        {
        ArrayList<ResourceEdge> ret = new ArrayList<>();
        Bag objs = ((Continuous2D)field).getAllObjects();
        for(int i = 0; i < objs.numObjs; i++)
            {
            Object obj = objs.objs[i];
            if (obj instanceof Provider)
                ret.addAll(connect((Provider)obj));
            else if (obj instanceof Multi)
                ret.addAll(connect((Multi)obj));
            else if (obj instanceof Macro)
                ret.addAll(connect((Macro)obj));
            }
        return ret;
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
