/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des.network;

import sim.field.network.*;
import sim.util.*;
import sim.engine.*;
import java.util.*;
import sim.des.*;

/**
   A subclass of Edge which allows the display and weighting of resources which
   travel from a provider to a receiver.
**/

public class ResourceEdge extends Edge
	{
    private static final long serialVersionUID = 1;

	public ResourceEdge(Provider provider, Receiver receiver, Network network)
		{
		super(provider, receiver, null);		// we have a null info because we grab it from the provider
		}
		
	public Object getInfo() 
		{ 
		Provider provider = (Provider)getFrom();
		Receiver receiver = (Receiver)getTo();
		double offerTime = provider.getLastAcceptedOfferTime();
		if (offerTime > Schedule.BEFORE_SIMULATION)
			{
			ArrayList<Resource> offers = provider.getLastAcceptedOffers();
			ArrayList<Receiver> receivers = provider.getLastAcceptedOfferReceivers();
			int loc = receivers.indexOf(receiver);
			if (loc >= 0)
				{
				if (offerTime == provider.getState().schedule.getTime())
					return "-->" + offers.get(loc);
				else return offers.get(loc);
				}
			else 
				{
				return null;
				}
			}
		else
			{
			return null;		// or maybe an empty object?
			}
		}
	}